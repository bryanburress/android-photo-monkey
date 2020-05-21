package com.chesapeaketechnology.photomonkey.loc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.annotation.Nullable;

import static android.content.Context.LOCATION_SERVICE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.LOCATION_REFRESH_DISTANCE;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.LOCATION_REFRESH_TIME;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PERMISSIONS_REQUEST_CODE;


/**
 * Provides location updates for permitted listeners.
 *
 * @since 0.1.0
 */
public class LocationHelper {
    private static final String LOG_TAG = LocationHelper.class.getSimpleName();
    private static final int LOW_POWER_STEP_DOWN_MULTIPLIER = 10;

    private Context context;
    private LocationUpdateDelegate delegate;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public static interface LocationUpdateDelegate {
        void locationUpdated(Location location);
    }

    public LocationHelper(Context context) {
        this.context = context;
    }

    private void deregister(){
        if (locationManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                Log.w(LOG_TAG, "Failed to remove location listener", ex);
            } finally {
                locationListener = null;
                delegate = null;
                locationManager = null;
            }
        }
    }

    private @Nullable Location register(Criteria criteria, long minTime, float minDistance) {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                LocationHelper.this.delegate.locationUpdated(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }
            @Override
            public void onProviderDisabled(String provider) { }
        };

        // Ensure that we have permissions to track the devices location.
        if (ContextCompat.checkSelfPermission( context,android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission( context, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED)
        {
            if (locationManager != null) {
                String provider = locationManager.getBestProvider(criteria, true);
                if(provider == null) provider = LocationManager.GPS_PROVIDER;

                locationManager.requestLocationUpdates(provider,
                        minTime,
                        minDistance,
                        locationListener);

                // return the last known location for an initial value.
                return locationManager.getLastKnownLocation(provider);
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Log.e(LOG_TAG, "Permission denied for location data.");
                Toast.makeText(this.context, "Permission denied for location data.", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_CODE);
            }
        }
        return null;
    }

    /**
     * Start the LocationManager and send updates to the provided delegate.
     * @param delegate Accepts location updates
     */
    @Nullable
    public Location startUpdatingLocation(final LocationUpdateDelegate delegate) {
        this.delegate = delegate;

        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setCostAllowed(true);
        return register(criteria, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE);
    }

    /**
     * Deregister any listeners from the location service.
     */
    public void stopUpdatingLocation(){
        if (locationManager != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this.context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.removeUpdates(locationListener);
            } catch (Exception ex) {
                Log.w(LOG_TAG, "Failed to remove location listener", ex);
            } finally {
                locationListener = null;
                delegate = null;
                locationManager = null;
            }
        }
    }

    public @Nullable Location switchToHighAccuracyMode(){
        deregister();
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setCostAllowed(true);
        Log.i(LOG_TAG, "Switching to high accuracy location tracking.");
        return register(criteria, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE);
    }

    public @Nullable Location switchToPowerConservationMode(){
        deregister();
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setCostAllowed(true);
        Log.i(LOG_TAG, "Switching to coarse grained, low power, location tracking.");
        return register(criteria,
                LOCATION_REFRESH_TIME * LOW_POWER_STEP_DOWN_MULTIPLIER,
                LOCATION_REFRESH_DISTANCE * LOW_POWER_STEP_DOWN_MULTIPLIER);
    }
}