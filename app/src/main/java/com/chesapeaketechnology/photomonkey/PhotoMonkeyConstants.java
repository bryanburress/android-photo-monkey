package com.chesapeaketechnology.photomonkey;

import android.os.Environment;

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

    public static final String PHOTO_MONKEY_PICTURES_DIRECTORY = Environment.DIRECTORY_PICTURES + "/PhotoMonkey Pictures";
}
