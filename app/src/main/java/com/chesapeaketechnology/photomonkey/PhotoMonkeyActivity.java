package com.chesapeaketechnology.photomonkey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chesapeaketechnology.photomonkey.loc.LocationManager;
import com.chesapeaketechnology.photomonkey.loc.LocationManagerProvider;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

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
public class PhotoMonkeyActivity extends AppCompatActivity implements LocationManagerProvider
{
    private static final String TAG = PhotoMonkeyActivity.class.getSimpleName();
    private FrameLayout container;
    private LocationManager locationManager;


//    public static File getOutputDirectory(Context context) {
//        Context appContext = context.getApplicationContext();
//        File[] mediaDirs = context.getExternalMediaDirs();
//        if ( mediaDirs != null && mediaDirs.length > 0 ) {
//            File mediaDir = Arrays.stream(mediaDirs).filter(Objects::nonNull).findFirst().orElse(null);
//            if (mediaDir != null && mediaDir.exists()) {
//                return mediaDir;
//            }
//        }
//        return appContext.getFilesDir();
//    }

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
