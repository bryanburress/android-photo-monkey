package com.chesapeaketechnology.photomonkey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.chesapeaketechnology.photomonkey.loc.ILocationManagerProvider;
import com.chesapeaketechnology.photomonkey.loc.LocationManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

/**
 * The main activity for the Photo Monkey app. This activity acts as a holder for a series of
 * fragments that facilitate taking pictures, editing metadata, and managing assets.
 *
 * @since 0.2.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity implements ILocationManagerProvider
{
    private static final String TAG = PhotoMonkeyActivity.class.getSimpleName();
    private FrameLayout container;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        container.postDelayed(() -> container.setSystemUiVisibility(FLAGS_FULLSCREEN), IMMERSIVE_FLAG_TIMEOUT);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        // Capture volume down hardware button and ensure it is forwarded to fragments.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent = intent.putExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_VOLUME_DOWN);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        clearExternalCache(getApplicationContext());
    }

    /**
     * Remove any lingering files in the external cache directory. This would include any
     * temp files created as a part of sharing intents.
     *
     * @param context The application {@link Context}
     */
    private void clearExternalCache(Context context)
    {
        try
        {
            File cacheDir = context.getExternalCacheDir();
            if (cacheDir != null && cacheDir.isDirectory())
            {
                Path rootPath = Paths.get(cacheDir.getAbsolutePath());
                try (Stream<Path> walk = Files.walk(rootPath))
                {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .peek(f -> Log.i(TAG, "Removing external cache file: " + f.getAbsolutePath()))
                            .forEach(File::delete);
                }
            }
        } catch (Exception e)
        {
            Log.w(TAG, "Unable to complete clearing external cache", e);
        }
    }

    @Override
    public LocationManager getLocationManager()
    {
        if (locationManager == null)
        {
            locationManager = new LocationManager(this, getLifecycle());
        }
        return locationManager;
    }
}
