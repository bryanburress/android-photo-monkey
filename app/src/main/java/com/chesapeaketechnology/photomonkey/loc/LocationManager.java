package com.chesapeaketechnology.photomonkey.loc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.LOCATION_REFRESH_DISTANCE_METERS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.LOCATION_REFRESH_TIME_MS;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUEST_CODE;

import timber.log.Timber;

/**
 * Provides location updates for permitted listeners.
 * Automatically switches to low power mode when the app is in the background
 * and to high accuracy mode when the app is in the foreground.
 *
 * @since 0.2.0
 */
public class LocationManager implements LifecycleObserver
{
    private static final int LOW_POWER_STEP_DOWN_MULTIPLIER = 10;
    private final Context context;
    private final Lifecycle lifecycle;
    private android.location.LocationManager locationManager;
    private LocationListener locationListener;
    private List<ILocationUpdateListener> updateListeners = new ArrayList<>();

    /**
     * Create a new instance of a {@link LocationManager}
     *
     * @param context   the android {@link Context} to execute within
     * @param lifecycle the application {@link Lifecycle}.
     */
    public LocationManager(Context context, Lifecycle lifecycle)
    {
        this.context = context;
        this.lifecycle = lifecycle;
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void create()
    {
        locationManager = (android.location.LocationManager) context.getSystemService(LOCATION_SERVICE);
        Criteria criteria = getCriteria(LocationTrackingMode.LOW_POWER);
        register(criteria, LOCATION_REFRESH_TIME_MS, LOCATION_REFRESH_DISTANCE_METERS);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void resume()
    {
        switchTo(LocationManager.LocationTrackingMode.HIGH_ACCURACY);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void pause()
    {
        // TODO: 5/23/20 Do I need to slow down the updates on pause?
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void stop()
    {
        switchTo(LocationManager.LocationTrackingMode.LOW_POWER);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void destroy()
    {
        lifecycle.removeObserver(this);
        deregister();
        locationListener = null;
        locationManager = null;
        updateListeners = new ArrayList<>();
    }

    /**
     * Deregister internal listener
     */
    private void deregister()
    {
        if (locationManager != null)
        {
            try
            {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex)
            {
                Timber.w(ex, "Failed to remove location listener");
            }
        }
    }

    /**
     * Register internal listener for Android location change events.
     *
     * @param criteria    the {@link Criteria} for choosing the best location provider.
     * @param minTime     minimum time interval between location updates, in milliseconds
     * @param minDistance minimum distance between location updates, in meters
     */
    private void register(Criteria criteria, long minTime, float minDistance)
    {
        locationListener = new LocationListener()
        {
            @Override
            public void onLocationChanged(final Location location)
            {
                updateListeners.forEach(listener -> listener.locationUpdated(location));
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
            }

            @Override
            public void onProviderEnabled(String provider)
            {
            }

            @Override
            public void onProviderDisabled(String provider)
            {
            }
        };

        // Ensure that we have permissions to track the devices location.
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            if (locationManager != null)
            {
                String provider = locationManager.getBestProvider(criteria, true);
                if (provider == null) provider = android.location.LocationManager.GPS_PROVIDER;

                locationManager.requestLocationUpdates(provider,
                        minTime,
                        minDistance,
                        locationListener);
            }
        } else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION))
            {
                Timber.e("Permission denied for location data.");
                Toast.makeText(context, "Permission denied for location data.", Toast.LENGTH_LONG).show();
            } else
            {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    /**
     * Add a new {@link ILocationUpdateListener}
     *
     * @param listener the listener to add
     */
    public void addUpdateListener(ILocationUpdateListener listener)
    {
        updateListeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener the {@link ILocationUpdateListener} to remove
     */
    public void removeUpdateListener(ILocationUpdateListener listener)
    {
        updateListeners.remove(listener);
    }

    /**
     * Setup the appropriate criteria for the provided {@link LocationTrackingMode}.
     *
     * @param mode The {@link LocationTrackingMode} to use as the determinant for creating the criteria.
     * @return a {@link Criteria} object for use in choosing a location provider.
     */
    private Criteria getCriteria(LocationTrackingMode mode)
    {
        Criteria criteria = new Criteria();
        switch (mode)
        {
            case HIGH_ACCURACY:
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setCostAllowed(true);
                criteria.setAltitudeRequired(true);
                criteria.setBearingRequired(true);
                criteria.setSpeedRequired(true);
                break;
            case LOW_POWER:
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                criteria.setCostAllowed(false);
                break;
        }
        return criteria;
    }

    /**
     * Provides a multiplier (or 1) that is used to reduce refresh rate
     * and refresh distance when in low power mode.
     *
     * @param mode The {@link LocationTrackingMode} to use as the determinant for the step down
     * @return an int representing a frequency multiplier
     */
    private int getStepDownMultiplier(LocationTrackingMode mode)
    {
        if (mode == LocationTrackingMode.LOW_POWER)
        {
            return LOW_POWER_STEP_DOWN_MULTIPLIER;
        }
        return 1;
    }

    /**
     * Switch between location tracking modes
     *
     * @param mode The {@link LocationTrackingMode} to which to switch
     */
    private void switchTo(LocationTrackingMode mode)
    {
        deregister();
        Criteria criteria = getCriteria(mode);
        Timber.i("Switching to location tracking mode %s.", mode.name());
        register(criteria,
                LOCATION_REFRESH_TIME_MS * (long) getStepDownMultiplier(mode),
                LOCATION_REFRESH_DISTANCE_METERS * (float) getStepDownMultiplier(mode));
    }

    public enum LocationTrackingMode
    {
        HIGH_ACCURACY,
        LOW_POWER
    }
}