package com.chesapeaketechnology.photomonkey;

import android.content.Context;
import android.provider.MediaStore;

public class PhotoMonkeyConfig {

    /**
     * Setting indicating that you would like to store images to the public external media
     * directory. This directory is defined by {@link Context#getExternalMediaDirs()}. This
     * is likely the setting that will work best with SyncMonkey.
     *
     * Setting USE_EXTERNAL_MEDIA_DIR to false will utilize the MediaStore APIs writing the
     * images to the directory defined by ({@link MediaStore#VOLUME_EXTERNAL_PRIMARY}
     * (likely the /Pictures folder).
     */
    public static final boolean USE_EXTERNAL_MEDIA_DIR = true;

    /**
     * Setting indicating that you would like for the application to automatically send the
     * photo to SyncMonkey (via intent).
     */
    public static final boolean AUTOMATIC_SEND_TO_SYNC_MONKEY = true;

}
