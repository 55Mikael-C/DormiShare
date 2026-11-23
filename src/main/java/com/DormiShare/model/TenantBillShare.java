package com.DormiShare.model;

public class TenantBillShare {
    private String tenantId;
    private String tenantName;
    private String roomId;
    private String roomName;
    private int billableDays;
    /** Denominator used: location tenant-days (Case A) or room tenant-days (Case B). */
    private int totalBasisDays;
    /** Case B only: room's consumption this period (0 if not sub-metered). */
    private double roomConsumption;
    /** Case B only: room's peso share before splitting among roommates (0 if not sub-metered). */
    private double roomPesoShare;
    private double amountPeso;

    public TenantBillShare() {}

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public int getBillableDays() { return billableDays; }
    public void setBillableDays(int billableDays) { this.billableDays = billableDays; }
    public int getTotalBasisDays() { return totalBasisDays; }
    public void setTotalBasisDays(int totalBasisDays) { this.totalBasisDays = totalBasisDays; }
    public double getRoomConsumption() { return roomConsumption; }
    public void setRoomConsumption(double roomConsumption) { this.roomConsumption = roomConsumption; }
    public double getRoomPesoShare() { return roomPesoShare; }
    public void setRoomPesoShare(double roomPesoShare) { this.roomPesoShare = roomPesoShare; }
    public double getAmountPeso() { return amountPeso; }
    public void setAmountPeso(double amountPeso) { this.amountPeso = amountPeso; }
}
