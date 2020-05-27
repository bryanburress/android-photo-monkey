package com.chesapeaketechnology.photomonkey.loc;

import android.location.Location;

/**
 * Provides the protocol by which objects can receive location updates.
 * @since 0.1.0
 */
public interface LocationUpdateListener {
    void locationUpdated(Location location);
}
