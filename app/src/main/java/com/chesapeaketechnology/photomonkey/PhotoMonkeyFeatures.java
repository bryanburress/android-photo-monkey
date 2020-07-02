package com.chesapeaketechnology.photomonkey;

import android.content.Context;
import android.provider.MediaStore;

/**
 * Used to provide build time feature flags for the application.
 *
 * @since 0.2.0
 */
public final class PhotoMonkeyFeatures
{
    /**
     * Setting indicating that you would like to store images to the public external media
     * directory. This directory is defined by {@link Context#getExternalMediaDirs()}
     * (Android/media/com.chesapeaketechnology.photomonkey/PhotoMonkey). This
     * is likely the setting that will work best with SyncMonkey.
     * <p>
     * Setting USE_EXTERNAL_MEDIA_DIR to false will utilize the MediaStore APIs writing the
     * images to the directory defined by ({@link MediaStore#VOLUME_EXTERNAL_PRIMARY}
     * (this is likely to go directly in the /Pictures folder).
     */
    public static final boolean USE_EXTERNAL_MEDIA_DIR = false;

    private PhotoMonkeyFeatures()
    {
    }

    /**
     * Setting indicating that you would like for the application to automatically send the
     * photo to SyncMonkey (via intent).
     */
    public static final boolean AUTOMATIC_SEND_TO_SYNC_MONKEY = false;
}
