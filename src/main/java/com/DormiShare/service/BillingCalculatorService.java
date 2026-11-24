package com.DormiShare.service;

import com.DormiShare.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BillingCalculatorService {
    
    public static boolean isSubMetered(Location loc, UtilityType type) {
        return type == UtilityType.ELECTRIC ? loc.isHasElectricSubMeter() : loc.isHasWaterSubMeter();
    }

    public static boolean isActiveOn(Tenant t, LocalDate d) {
        return isStaying(t, d);
    }

    private static boolean isStaying(Tenant t, LocalDate d) {
        if (t.getMoveInDate() == null || d.isBefore(t.getMoveInDate())) return false;
        return t.getMoveOutDate() == null || !d.isAfter(t.getMoveOutDate());
    }

    private static boolean isAway(Tenant t, LocalDate d) {
        if (t.getAwayPeriods() == null) return false;
        for (DateRange r : t.getAwayPeriods()) {
            if (r.getStart() != null && r.getEnd() != null
                    && !d.isBefore(r.getStart()) && !d.isAfter(r.getEnd())) return true;
        }
        return false;
    }

    /** Days (inclusive) the tenant lived there within the window, ignoring away periods. */
    public int stayDays(Tenant t, LocalDate start, LocalDate end) {
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isStaying(t, d)) n++;
        }
        return n;
    }

    /** Days the tenant lived there AND was not away. This is what utilities are split on. */
    public int billableDays(Tenant t, LocalDate start, LocalDate end) {
        int n = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isStaying(t, d) && !isAway(t, d)) n++;
        }
        return n;
    }

    /**
     * Pure calculation (no side effects), used for the live preview and by commit().
     * prev/curr are roomId -> meter reading; ignored when the utility isn't sub-metered.
     */
    public List<TenantBillShare> calculate(Location loc, UtilityType type, LocalDate start, LocalDate end,
                                           double totalBill, Map<String, Double> prev, Map<String, Double> curr) {
        if (start == null || end == null || end.isBefore(start))
            throw new IllegalArgumentException("Period end must be on or after period start.");
        if (totalBill <= 0)
            throw new IllegalArgumentException("Total bill must be greater than zero.");

        List<TenantBillShare> shares = new ArrayList<>();

        if (!isSubMetered(loc, type)) {
            // Case A: prorate by tenant-days across the whole location
            int totalDays = 0;
            for (Room room : loc.getRooms()) {
                for (Tenant t : room.getTenants()) {
                    if (stayDays(t, start, end) == 0) continue;
                    TenantBillShare s = newShare(room, t, billableDays(t, start, end));
                    shares.add(s);
                    totalDays += s.getBillableDays();
                }
            }
            if (totalDays == 0)
                throw new IllegalStateException("No billable tenant-days in this period.");
            for (TenantBillShare s : shares) {
                s.setTotalBasisDays(totalDays);
                s.setAmountPeso((double) s.getBillableDays() / totalDays * totalBill);
            }
        } else {
            // Case B: room consumption ratio, then tenant-days inside each room
            Map<String, Double> consumption = new LinkedHashMap<>();
            double totalCons = 0;
            for (Room room : loc.getRooms()) {
                Double p = prev.get(room.getId());
                Double c = curr.get(room.getId());
                if (p == null || c == null)
                    throw new IllegalArgumentException("Missing meter reading for room " + room.getName() + ".");
                if (c < p)
                    throw new IllegalArgumentException("Current reading is lower than previous for room " + room.getName() + ".");
                consumption.put(room.getId(), c - p);
                totalCons += c - p;
            }
            if (totalCons <= 0)
                throw new IllegalStateException("Total consumption is zero. Check the meter readings.");

            for (Room room : loc.getRooms()) {
                double cons = consumption.get(room.getId());
                double roomPeso = cons / totalCons * totalBill;
                List<Tenant> tenants = room.getTenants().stream()
                        .filter(t -> stayDays(t, start, end) > 0).toList();
                if (tenants.isEmpty()) {
                    if (cons > 0)
                        throw new IllegalStateException("Room " + room.getName() + " used " + cons
                                + " units but has no tenants in this period.");
                    continue;
                }
                int billableSum = 0, staySum = 0;
                for (Tenant t : tenants) {
                    billableSum += billableDays(t, start, end);
                    staySum += stayDays(t, start, end);
                }
                // Edge case: everyone in the room was away. Split by stay days so the room's share isn't lost.
                boolean fallback = billableSum == 0;
                int basis = fallback ? staySum : billableSum;
                for (Tenant t : tenants) {
                    int bd = billableDays(t, start, end);
                    int weight = fallback ? stayDays(t, start, end) : bd;
                    TenantBillShare s = newShare(room, t, bd);
                    s.setTotalBasisDays(basis);
                    s.setRoomConsumption(cons);
                    s.setRoomPesoShare(round2(roomPeso));
                    s.setAmountPeso((double) weight / basis * roomPeso);
                    shares.add(s);
                }
            }
        }

        // Round to centavos and push any rounding remainder onto the largest share
        // so the shares always add up exactly to the bill.
        double rounded = 0;
        for (TenantBillShare s : shares) {
            s.setAmountPeso(round2(s.getAmountPeso()));
            rounded += s.getAmountPeso();
        }
        double diff = round2(totalBill - rounded);
        if (diff != 0 && !shares.isEmpty()) {
            TenantBillShare biggest = Collections.max(shares, Comparator.comparingDouble(TenantBillShare::getAmountPeso));
            biggest.setAmountPeso(round2(biggest.getAmountPeso() + diff));
        }
        return shares;
    }

    /** Calculates, then records the bill, updates last meter readings, and marks affected tenants' utilities PENDING. */
    public BillRecord commit(Location loc, UtilityType type, LocalDate start, LocalDate end,
                             double totalBill, Map<String, Double> prev, Map<String, Double> curr) {
        List<TenantBillShare> shares = calculate(loc, type, start, end, totalBill, prev, curr);

        BillRecord rec = new BillRecord();
        rec.setType(type);
        rec.setPeriodStart(start);
        rec.setPeriodEnd(end);
        rec.setTotalBillPeso(totalBill);
        rec.setTenantShares(shares);

        if (isSubMetered(loc, type)) {
            rec.getRoomReadings().putAll(curr);
            rec.getRoomPreviousReadings().putAll(prev);
            for (Room room : loc.getRooms()) {
                Double c = curr.get(room.getId());
                if (c == null) continue;
                if (type == UtilityType.ELECTRIC) room.setLastElectricReading(c);
                else room.setLastWaterReading(c);
            }
        }

        Set<String> billed = shares.stream().map(TenantBillShare::getTenantId).collect(Collectors.toSet());
        for (Room room : loc.getRooms())
            for (Tenant t : room.getTenants())
                if (billed.contains(t.getId())) t.setUtilityStatus(PaymentStatus.PENDING);

        loc.getBillHistory().add(rec);
        return rec;
    }

    private TenantBillShare newShare(Room room, Tenant t, int billableDays) {
        TenantBillShare s = new TenantBillShare();
        s.setTenantId(t.getId());
        s.setTenantName(t.getFullName());
        s.setRoomId(room.getId());
        s.setRoomName(room.getName());
        s.setBillableDays(billableDays);
        return s;
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
