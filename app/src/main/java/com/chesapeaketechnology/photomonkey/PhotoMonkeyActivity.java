package com.chesapeaketechnology.photomonkey;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.chesapeaketechnology.photomonkey.loc.ILocationManagerProvider;
import com.chesapeaketechnology.photomonkey.loc.LocationManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

import timber.log.Timber;

/**
 * The main activity for the Photo Monkey app. This activity acts as a holder for a series of
 * fragments that facilitate taking pictures, editing metadata, and managing assets.
 *
 * @since 0.2.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity implements ILocationManagerProvider
{
    private FrameLayout container;
    private LocationManager locationManager;
    private BroadcastReceiver managedConfigurationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
        managedConfigurationListener = registerManagedConfigurationListener(getApplicationContext());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        container.postDelayed(() -> container.setSystemUiVisibility(FLAGS_FULLSCREEN), IMMERSIVE_FLAG_TIMEOUT);

        // Per the Android developer tutorials it is recommended to read the managed configuration in the onResume method
        readPhotoMonkeyManagedConfiguration(this);
        managedConfigurationListener = registerManagedConfigurationListener(getApplicationContext());
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
        unregisterManagedConfigurationListener();
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
                            .peek(f -> Timber.i("Removing external cache file: %s", f.getAbsolutePath()))
                            .forEach(File::delete);
                }
            }
        } catch (Exception e)
        {
            Timber.w(e, "Unable to complete clearing external cache");
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

    /**
     * Reads the Sync Monkey Managed Configuration and loads the values into the App's Shared Preferences.
     */
    @SuppressLint("ApplySharedPref")
    public void readPhotoMonkeyManagedConfiguration(Context context)
    {
        try
        {
            // Next, read any MDM set values.  Doing this last so that we can overwrite the values from the properties file
            Timber.i("Reading in any MDM configured properties");
            final RestrictionsManager restrictionsManager = (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
            if (restrictionsManager != null)
            {
                final Bundle mdmProperties = restrictionsManager.getApplicationRestrictions();

                final boolean mdmOverride = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PROPERTY_MDM_OVERRIDE_KEY, false);

                Timber.d("When reading the Photo Monkey managed configuration the mdmOverride=%s", mdmOverride);

                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
                mdmProperties.keySet().forEach(key -> {
                    final Object property = mdmProperties.get(key);

                    if (property instanceof String)
                    {
                        edit.putString(key, (String) property);
                    }
                });
                edit.commit();
            }
        } catch (Exception e)
        {
            Timber.e(e, "Can't read the Sync Monkey managed configuration");
        }
    }

    /**
     * Register a listener so that if the Managed Config changes we will be notified of the new config.
     */
    public BroadcastReceiver registerManagedConfigurationListener(Context context)
    {
        Timber.e("Registering the managed conf listener");
        final IntentFilter restrictionsFilter = new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED);

        final BroadcastReceiver restrictionsReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                readPhotoMonkeyManagedConfiguration(context);
            }
        };

        context.registerReceiver(restrictionsReceiver, restrictionsFilter);

        return restrictionsReceiver;
    }

    /**
     * Remove the managed configuration listener.
     *
     * @since 0.2.1
     */
    private void unregisterManagedConfigurationListener()
    {
        if (managedConfigurationListener != null)
        {
            try
            {
                getApplicationContext().unregisterReceiver(managedConfigurationListener);
            } catch (Exception e)
            {
                Timber.e(e, "Unable to unregister the Managed Configuration Listener when pausing the app");
            }
            managedConfigurationListener = null;
        }
    }
}
