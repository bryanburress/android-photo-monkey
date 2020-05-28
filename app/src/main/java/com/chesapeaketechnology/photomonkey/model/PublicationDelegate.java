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
 * @since 0.2.0
 */
public class PublicationDelegate {
    private static final String TAG = PublicationDelegate.class.getSimpleName();

    /**
     * Scan the media to ensure other apps can see the image.
     *
     * @param image
     */
    public void makeAvailableToOtherApps(Image image) {
        //Use the media scanner to ensure other apps can see the image.
        File file = image.getFile();
        //noinspection UnstableApiUsage
        String extension = Files.getFileExtension(file.getName());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        MediaScannerConnection.scanFile(PhotoMonkeyApplication.getContext(), new String[]{file.getAbsolutePath()}, new String[]{mimeType}, (path, uri) -> {
            Log.d(TAG, String.format("Image capture scanned into media store: %s", uri));
        });
    }

    /**
     * Send the image to SyncMonkey via the SEND_FILE_NO_UI action.
     *
     * @param image
     * @throws PublicationFailure
     */
    public void sendToSyncMonkey(Image image) throws PublicationFailure {
        final Intent syncMonkeyIntent = new Intent(PhotoMonkeyConstants.SYNC_MONKEY_ACTION);
        syncMonkeyIntent.addCategory(Intent.CATEGORY_DEFAULT);
        syncMonkeyIntent.setComponent(new ComponentName(PhotoMonkeyConstants.SYNC_MONKEY_PACKAGE, PhotoMonkeyConstants.SYNC_MONKEY_SHARING_ACTIVITY_CLASS));
        syncMonkeyIntent.setType("image/jpg");
        syncMonkeyIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        syncMonkeyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        syncMonkeyIntent.putExtra(Intent.EXTRA_STREAM, image.getUri());
        try {
            PhotoMonkeyApplication.getContext().startActivity(syncMonkeyIntent);
        } catch (ActivityNotFoundException e) {
            throw new PublicationFailure("Unable to find the Sync Monkey SharingActivity.  Only saving the file locally.", e);
        }
    }


    public static class PublicationFailure extends Exception {
        public PublicationFailure(String message) {
            super(message);
        }

        public PublicationFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
