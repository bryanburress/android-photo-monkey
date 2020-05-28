package com.chesapeaketechnology.photomonkey;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chesapeaketechnology.photomonkey.loc.LocationManager;
import com.chesapeaketechnology.photomonkey.loc.LocationManagerProvider;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FLAGS_FULLSCREEN;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.IMMERSIVE_FLAG_TIMEOUT;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_EXTRA;


/**
 * The main activity for the Photo Monkey app. This activity acts as a holder for a series of
 * fragments that facilitate taking pictures, editing metadata, and managing assets.
 *
 * @since 0.2.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity implements LocationManagerProvider {
    private FrameLayout container;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(() -> container.setSystemUiVisibility(FLAGS_FULLSCREEN), IMMERSIVE_FLAG_TIMEOUT);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Capture volume down hardware button and ensure it is forwarded to fragments.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent = intent.putExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_VOLUME_DOWN);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public LocationManager getLocationManager() {
        if (locationManager == null) {
            locationManager = new LocationManager(this, getLifecycle());
        }
        return locationManager;
    }
}
