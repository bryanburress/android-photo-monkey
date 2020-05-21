package com.chesapeaketechnology.photomonkey.sdata;

import android.location.Location;

import java.util.Objects;

public class SupplementaryData {

    private final String description;
    private final Location location;

    public SupplementaryData(String description, Location location) {

        this.description = description;
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplementaryData)) return false;
        SupplementaryData that = (SupplementaryData) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, location);
    }

    @Override
    public String toString() {
        return "SupplementaryData{" +
                "description='" + description + '\'' +
                ", location=" + location +
                '}';
    }
}
