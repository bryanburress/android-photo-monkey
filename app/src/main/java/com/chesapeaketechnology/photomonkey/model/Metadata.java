package com.chesapeaketechnology.photomonkey.model;

import android.location.Location;

import androidx.annotation.NonNull;

/**
 * The metedata for an image
 *
 * @since 0.2.0
 */
public class Metadata
{
    private final Location location;
    private final boolean reversed;
    private final String description;

    /**
     * Create a new metadata object.
     *
     * @param description The image description.
     * @param location    The location the image was taken.
     * @param reversed    Is the image reversed (lens).
     */
    public Metadata(String description, Location location, boolean reversed)
    {
        this.location = location;
        this.reversed = reversed;
        this.description = description;
    }

    /**
     * Get the location the image was taken.
     *
     * @return a {@link Location} object representing the location the image was taken.
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * Get the description of the image.
     *
     * @return a {@link String} description of the image.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Is the image reversed (used the front facing lens)?
     *
     * @return a boolean value representing whether the image is reversed.
     */
    public boolean isReversed()
    {
        return reversed;
    }

    /**
     * Generate a string representation of this metadata object.
     *
     * @return a String.
     */
    @Override
    public @NonNull
    String toString()
    {
        return "Metadata{" +
                "location=" + location +
                ", reversed=" + reversed +
                ", description='" + description + '\'' +
                '}';
    }
}
