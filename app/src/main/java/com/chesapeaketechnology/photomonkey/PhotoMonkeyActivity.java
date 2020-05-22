package com.chesapeaketechnology.photomonkey;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chesapeaketechnology.photomonkey.loc.LocationHelper;
import com.chesapeaketechnology.photomonkey.loc.LocationUpdateListener;
import com.chesapeaketechnology.photomonkey.loc.LocationUpdateProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FLAGS_FULLSCREEN;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.IMMERSIVE_FLAG_TIMEOUT;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_EXTRA;


/**
 * The main activity for the Photo Monkey app.  This activity first launches the Android device's default camera app,
 * and once the user takes a picture they are shown the picture to add any details.
 *
 * @since 0.1.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity implements LocationUpdateProvider
{
    private static final String LOG_TAG = PhotoMonkeyActivity.class.getSimpleName();
    private FrameLayout container;
    private LocationHelper locationHelper;
    private List<LocationUpdateListener> locationUpdateListeners = new ArrayList<>();
    private Location lastLocation;

    public static File getOutputDirectory(Context context) {
        Context appContext = context.getApplicationContext();
        File[] mediaDirs = context.getExternalMediaDirs();
        if ( mediaDirs != null && mediaDirs.length > 0 ) {
            File mediaDir = Arrays.stream(mediaDirs).filter(Objects::nonNull).findFirst().orElse(null);
            if (mediaDir != null && mediaDir.exists()) {
                return mediaDir;
            }
        }
        return appContext.getFilesDir();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Register for location updates
        if (locationHelper == null) {
            locationHelper = new LocationHelper(this);
            lastLocation = locationHelper.startUpdatingLocation(location -> {
                locationUpdateListeners.forEach(listener -> {
                    listener.locationUpdated(location);
                });
            });
        } else {
            locationHelper.switchTo(LocationHelper.LocationTrackingMode.HIGH_ACCURACY);
        }

        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                container.setSystemUiVisibility(FLAGS_FULLSCREEN);
            }
        }, IMMERSIVE_FLAG_TIMEOUT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationHelper != null) {
            locationHelper.switchTo(LocationHelper.LocationTrackingMode.LOW_POWER);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Deregister any location services
        if(locationHelper != null) {
            locationHelper.stopUpdatingLocation();
        }
        locationHelper = null;
        locationUpdateListeners = new ArrayList<>();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent = intent.putExtra(KEY_EVENT_EXTRA, keyCode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Nullable
    @Override
    public Location addLocationUpdateListener(LocationUpdateListener delegate) {
        locationUpdateListeners.add(delegate);
        return lastLocation;
    }

    @Override
    public void removeLocationUpdateListener(LocationUpdateListener delegate) {
        locationUpdateListeners.remove(delegate);
    }
}
