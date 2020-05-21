package com.chesapeaketechnology.photomonkey.image;

import android.location.Location;

public interface ImageDataResultListener {
    public void onData(String description, Location location);
}
