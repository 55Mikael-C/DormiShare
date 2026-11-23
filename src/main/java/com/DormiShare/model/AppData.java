package com.DormiShare.model;

import java.util.ArrayList;
import java.util.List;

public class AppData {
    private List<Location> locations = new ArrayList<>();

    public List<Location> getLocations() { return locations; }
    public void setLocations(List<Location> locations) { this.locations = locations; }
}
