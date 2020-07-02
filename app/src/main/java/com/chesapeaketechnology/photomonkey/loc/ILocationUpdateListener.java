package com.chesapeaketechnology.photomonkey.loc;

import android.location.Location;

/**
 * Provides the protocol by which objects can receive location updates.
 *
 * @since 0.2.0
 */
public interface ILocationUpdateListener
{
    /**
     * Called when a location update has been received.
     *
     * @param location a {@link Location} object.
     */
    void locationUpdated(Location location);
}
