package com.DormiShare.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BillRecord {
    private String id = UUID.randomUUID().toString();
    private UtilityType type;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double totalBillPeso;
    /** roomId -> current reading (only if sub-metered). */
    private Map<String, Double> roomReadings = new HashMap<>();
    /** roomId -> previous reading (only if sub-metered). */
    private Map<String, Double> roomPreviousReadings = new HashMap<>();
    private List<TenantBillShare> tenantShares = new ArrayList<>();

    public BillRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UtilityType getType() { return type; }
    public void setType(UtilityType type) { this.type = type; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public double getTotalBillPeso() { return totalBillPeso; }
    public void setTotalBillPeso(double totalBillPeso) { this.totalBillPeso = totalBillPeso; }
    public Map<String, Double> getRoomReadings() { return roomReadings; }
    public void setRoomReadings(Map<String, Double> roomReadings) { this.roomReadings = roomReadings; }
    public Map<String, Double> getRoomPreviousReadings() { return roomPreviousReadings; }
    public void setRoomPreviousReadings(Map<String, Double> m) { this.roomPreviousReadings = m; }
    public List<TenantBillShare> getTenantShares() { return tenantShares; }
    public void setTenantShares(List<TenantBillShare> tenantShares) { this.tenantShares = tenantShares; }
}