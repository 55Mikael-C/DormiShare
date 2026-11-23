package com.DormiShare.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tenant {
    private String id = UUID.randomUUID().toString();
    private String fullName;
    private LocalDate moveInDate;
    private LocalDate moveOutDate; // null = still staying
    private List<DateRange> awayPeriods = new ArrayList<>();
    private PaymentStatus rentStatus = PaymentStatus.PENDING;
    private PaymentStatus utilityStatus = PaymentStatus.PENDING;

    public Tenant() {}

    public Tenant(String fullName, LocalDate moveInDate, LocalDate moveOutDate) {
        this.fullName = fullName;
        this.moveInDate = moveInDate;
        this.moveOutDate = moveOutDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getMoveInDate() { return moveInDate; }
    public void setMoveInDate(LocalDate moveInDate) { this.moveInDate = moveInDate; }
    public LocalDate getMoveOutDate() { return moveOutDate; }
    public void setMoveOutDate(LocalDate moveOutDate) { this.moveOutDate = moveOutDate; }
    public List<DateRange> getAwayPeriods() { return awayPeriods; }
    public void setAwayPeriods(List<DateRange> awayPeriods) { this.awayPeriods = awayPeriods; }
    public PaymentStatus getRentStatus() { return rentStatus; }
    public void setRentStatus(PaymentStatus rentStatus) { this.rentStatus = rentStatus; }
    public PaymentStatus getUtilityStatus() { return utilityStatus; }
    public void setUtilityStatus(PaymentStatus utilityStatus) { this.utilityStatus = utilityStatus; }

    @Override
    public String toString() { return fullName; }
}
