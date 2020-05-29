package com.chesapeaketechnology.photomonkey.view;

import android.location.Location;

import androidx.lifecycle.ViewModel;

import com.chesapeaketechnology.photomonkey.loc.ILocationUpdateListener;
import com.chesapeaketechnology.photomonkey.loc.LocationManager;
import com.chesapeaketechnology.photomonkey.model.Image;

/**
 * Used to share data between the various {@link androidx.fragment.app.Fragment}s.
 *
 * @since 0.2.0
 */
public class SharedImageViewModel extends ViewModel implements ILocationUpdateListener
{
    private Image image;
    private LocationManager locationManager;
    private Location lastLocation;
    private boolean isReversed;

    public SharedImageViewModel()
    {
    }

    public Image getImage()
    {
        return image;
    }

    public void setImage(Image image)
    {
        this.image = image;
    }

    public Location getLastLocation()
    {
        return lastLocation;
    }

    public boolean isReversed()
    {
        return isReversed;
    }

    public void setReversed(boolean reversed)
    {
        isReversed = reversed;
    }

    public void setLocationManager(LocationManager locationManager)
    {
        this.locationManager = locationManager;
        locationManager.addUpdateListener(this);
    }

    // TODO: 5/23/20 Where to remove listener?

    @Override
    public void locationUpdated(Location location)
    {
        lastLocation = location;
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        locationManager.removeUpdateListener(this);
    }
}
