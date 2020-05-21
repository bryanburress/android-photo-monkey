package com.chesapeaketechnology.photomonkey.loc;

import android.location.Location;

import javax.annotation.Nullable;

public interface LocationUpdateProvider {

    @Nullable Location addLocationUpdateListener(LocationUpdateListener delegate);

    void removeLocationUpdateListener(LocationUpdateListener delegate);
}
