package com.DormiShare.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    private String id = UUID.randomUUID().toString(); //not sure if need pa toh
    private String name;
    private int capacity;
    private double monthlyRent;
    private double lastElectricReading;
    private double lastWaterReading;
    private List<Tenant> tenants = new ArrayList<>();

    public Room() {}

    public Room(String name, int capacity, double monthlyRent) {
        this.name = name;
        this.capacity = capacity;
        this.monthlyRent = monthlyRent;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public double getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(double monthlyRent) { this.monthlyRent = monthlyRent; }
    public double getLastElectricReading() { return lastElectricReading; }
    public void setLastElectricReading(double v) { this.lastElectricReading = v; }
    public double getLastWaterReading() { return lastWaterReading; }
    public void setLastWaterReading(double v) { this.lastWaterReading = v; }
    public List<Tenant> getTenants() { return tenants; }
    public void setTenants(List<Tenant> tenants) { this.tenants = tenants; }

    @Override
    public String toString() { return name; } // Later for JavaFX
}
