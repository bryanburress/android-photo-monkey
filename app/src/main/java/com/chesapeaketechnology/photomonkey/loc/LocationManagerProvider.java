package com.chesapeaketechnology.photomonkey.loc;

/**
 * Marker interface to centralize the LocationManager while
 * still allowing related Fragments to have access.
 * @since 0.1.0
 */
public interface LocationManagerProvider {
    LocationManager getLocationManager();
}
