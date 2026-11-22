package com.DormiShare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Location {
    private String id = UUID.randomUUID().toString(); // Para Unique ang each location, namechange roomchange man yan
    private String name;
    private boolean hasElectricSubMeter;
    private boolean hasWaterSubMeter;
    private List<Room> rooms = new ArrayList<>();
    private List<BillRecord> billHistory = new ArrayList<>();

    public Location() {}

    public Location(String name, boolean hasElectricSubMeter, boolean hasWaterSubMeter) {
        this.name = name;
        this.hasElectricSubMeter = hasElectricSubMeter;
        this.hasWaterSubMeter = hasWaterSubMeter;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isHasElectricSubMeter() { return hasElectricSubMeter; }
    public void setHasElectricSubMeter(boolean v) { this.hasElectricSubMeter = v; }
    public boolean isHasWaterSubMeter() { return hasWaterSubMeter; }
    public void setHasWaterSubMeter(boolean v) { this.hasWaterSubMeter = v; }
    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
    public List<BillRecord> getBillHistory() { return billHistory; }
    public void setBillHistory(List<BillRecord> billHistory) { this.billHistory = billHistory; }

    @Override
    public String toString() { return name; } // Later for JavaFX
}
