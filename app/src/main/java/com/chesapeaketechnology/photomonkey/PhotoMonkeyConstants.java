package com.chesapeaketechnology.photomonkey;

import android.Manifest;
import android.view.View;

import java.time.format.DateTimeFormatter;

public final class PhotoMonkeyConstants
{
    private PhotoMonkeyConstants()
    {
    }

    /**
     * The authority for the file provider.
     */
    public static final String AUTHORITY = "com.chesapeaketechnology.photomonkey.fileprovider";

    /**
     * The prefix to add the be beginning of each photo taken with this app.
     */
    public static final String PHOTO_MONKEY_PHOTO_NAME_PREFIX = "PM_";

    public static int FLAGS_FULLSCREEN =
        View.SYSTEM_UI_FLAG_LOW_PROFILE |
        View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    public static final int PERMISSIONS_REQUEST_CODE = 2;
    public static final String[] PERMISSIONS_REQUIRED = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final String KEY_EVENT_ACTION = "key_event_action";
    public static final String KEY_EVENT_EXTRA = "key_event_extra";
    public static final long IMMERSIVE_FLAG_TIMEOUT = 500L;

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
}
