package com.chesapeaketechnology.photomonkey;

import android.Manifest;
import android.util.Size;
import android.view.View;

import java.util.Collections;
import java.util.List;

/**
 * Provides constant values used to configure various aspects of the application.
 *
 * @since 0.2.0
 */
public final class PhotoMonkeyConstants
{
    /**
     * The prefix to add the be beginning of each photo taken with this app.
     */
    public static final String PHOTO_MONKEY_PHOTO_NAME_PREFIX = "PM_";
    /**
     * Flags used to ensure the views are in full screen mode
     */
    public static final int FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    /**
     * Delay interval for setting the full screen flags after the activity has loaded
     */
    public static final long IMMERSIVE_FLAG_TIMEOUT = 500L;
    /**
     * Unique code used for requesting permissions.
     */
    public static final int PERMISSIONS_REQUEST_CODE = 2;
    /**
     * System permissions required by the application to function correctly
     */
    public static final String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    /**
     * Used as part of relaying volume key press events to the fragements in the application
     */
    public static final String KEY_EVENT_ACTION = "key_event_action";
    // TODO: 5/29/20 Look into adding Manifest.permission.ACCESS_MEDIA_LOCATION permission per https://code.luasoftware.com/tutorials/android/get-photo-gps-location-with-android-10-scoped-storage/  
    /**
     * Used to filter media files in the gallery to jpeg images
     */
    public static final List<String> EXTENSION_WHITELIST = Collections.singletonList("JPG");
    public static final String KEY_EVENT_EXTRA = "key_event_extra";
    /**
     * Used for the portrait and landscape aspect ratio in the image preview and camera capture
     */
    public static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    public static final double RATIO_16_9_VALUE = 16.0 / 9.0;
    /**
     * Timing setting for effects related to the camera.
     */
    public static final long ANIMATION_FAST_MILLIS = 100;
    public static final long ANIMATION_SLOW_MILLIS = 250;

    /**
     * Attributed related to the on screen focus rectangle
     */
    public static final int FOCUS_STROKE_WIDTH = 8;
    public static final int FOCUS_RECT_WIDTH = 250;
    public static final int FOCUS_RECT_HEIGHT = 250;
    public static final Size FOCUS_RECT_SIZE = new Size(FOCUS_RECT_WIDTH, FOCUS_RECT_HEIGHT);

    /**
     * The minimum number of milliseconds to wait between location update. Used for high accuracy mode.
     */
    public static final int LOCATION_REFRESH_TIME = 3000; // 3 seconds.
    /**
     * The minimum distance traveled before a location update. Used for high accuracy mode.
     */
    public static final int LOCATION_REFRESH_DISTANCE = 30; // 30 meters.
    /**
     * Timeouts used for asynchronous blocking access to media files
     */
    public static final int SINGLE_FILE_IO_TIMEOUT = 10; //in seconds
    /**
     * Configurations for SyncMonkey sharing intents
     */
    public static final String SYNC_MONKEY_ACTION = "com.chesapeaketechnology.sycnmonkey.action.SEND_FILE_NO_UI";
    public static final int MULTI_FILE_IO_TIMEOUT = 30; //in seconds

    private PhotoMonkeyConstants()
    {
    }

    public static final String SYNC_MONKEY_PACKAGE = "com.chesapeaketechnology.syncmonkey";
    public static final String SYNC_MONKEY_SHARING_ACTIVITY_CLASS = "com.chesapeaketechnology.syncmonkey.SharingActivity";
}
