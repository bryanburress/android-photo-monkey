package com.chesapeaketechnology.photomonkey.model;

import android.location.Location;

public class Metadata {
    private Location location;
    private boolean reversed;
    private String description;

    public Metadata(){}

    public Metadata(String description, Location location, boolean reversed) {
        this.location = location;
        this.reversed = reversed;
        this.description = description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "location=" + location +
                ", reversed=" + reversed +
                ", description='" + description + '\'' +
                '}';
    }
}
