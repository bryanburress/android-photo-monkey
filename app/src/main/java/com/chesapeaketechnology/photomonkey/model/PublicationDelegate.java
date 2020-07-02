package com.chesapeaketechnology.photomonkey.model;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants;
import com.google.common.io.Files;

import java.io.File;

/**
 * Encapsulates functionality related to notifying other applications of
 * image changes and sharing an image with other applications.
 *
 * @since 0.1.0
 */
public class PublicationDelegate
{
    private static final String TAG = PublicationDelegate.class.getSimpleName();

    /**
     * Scan the media to ensure other apps can see the image.
     *
     * @param image The Image to make available.
     */
    public void makeAvailableToOtherApps(Image image)
    {
        //Use the media scanner to ensure other apps can see the image.
        File file = image.getFile();
        //noinspection UnstableApiUsage
        String extension = Files.getFileExtension(file.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        MediaScannerConnection.scanFile(PhotoMonkeyApplication.getContext(),
                new String[]{file.getAbsolutePath()}, new String[]{mimeType},
                (path, uri) -> Log.d(TAG, String.format("Image capture scanned into media store: %s", uri)));
    }

    /**
     * Send the image to SyncMonkey via the SEND_FILE_NO_UI action.
     *
     * @param image The image to send ot Sync Monkey.
     * @throws PublicationFailure if the Sync Monkey sharing activity could not be found.
     */
    public void sendToSyncMonkey(Image image) throws PublicationFailure
    {
        final Intent syncMonkeyIntent = new Intent(PhotoMonkeyConstants.SYNC_MONKEY_ACTION_SEND_FILE_NO_UI);
        syncMonkeyIntent.addCategory(Intent.CATEGORY_DEFAULT);
        syncMonkeyIntent.setComponent(new ComponentName(PhotoMonkeyConstants.SYNC_MONKEY_PACKAGE, PhotoMonkeyConstants.SYNC_MONKEY_SHARING_ACTIVITY_CLASS));
        syncMonkeyIntent.setType("image/jpg");
        syncMonkeyIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        syncMonkeyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        syncMonkeyIntent.putExtra(Intent.EXTRA_STREAM, image.getUri());
        try
        {
            PhotoMonkeyApplication.getContext().startActivity(syncMonkeyIntent);
        } catch (ActivityNotFoundException e)
        {
            throw new PublicationFailure("Unable to find the Sync Monkey SharingActivity.  Only saving the file locally.", e);
        }
    }

    /**
     * Fire an intent to Sync Monkey telling it to perform a sync. This is helpful after a new
     * image has been saved to the Photo Monkey directory so that the image will show up on the
     * remote server immediately.
     */
    public static void kickOffSyncMonkeySync()
    {
        try
        {
            final Intent syncMonkeyIntent = new Intent(PhotoMonkeyConstants.SYNC_MONKEY_ACTION_SYNC_NOW);
            syncMonkeyIntent.setComponent(new ComponentName(PhotoMonkeyConstants.SYNC_MONKEY_PACKAGE, PhotoMonkeyConstants.SYNC_MONKEY_BROADCAST_RECEIVER_CLASS));
            PhotoMonkeyApplication.getContext().sendBroadcast(syncMonkeyIntent);
        } catch (Exception e)
        {
            Log.i(TAG, "Something went wrong when trying to kick off a sync with Sync Monkey.");
        }
    }

    /**
     * Indicates there was an error publishing the image.
     */
    public static class PublicationFailure extends Exception
    {
        public PublicationFailure(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
