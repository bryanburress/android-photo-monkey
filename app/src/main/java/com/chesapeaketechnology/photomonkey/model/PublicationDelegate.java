package com.chesapeaketechnology.photomonkey.model;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication.getContext;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants;
import com.chesapeaketechnology.photomonkey.service.PhotoUploadSubmissionHandler;
import com.chesapeaketechnology.photomonkey.util.PreferenceUtils;
import com.google.common.io.Files;

import java.io.File;

import timber.log.Timber;

/**
 * Encapsulates functionality related to notifying other applications of
 * image changes and sharing an image with other applications.
 *
 * @since 0.1.0
 */
public class PublicationDelegate
{
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
        MediaScannerConnection.scanFile(getContext(),
                new String[]{file.getAbsolutePath()}, new String[]{mimeType},
                (path, uri) -> Timber.d("Image capture scanned into media store: %s", uri));
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
            getContext().startActivity(syncMonkeyIntent);
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
            getContext().sendBroadcast(syncMonkeyIntent);
        } catch (Exception e)
        {
            Timber.i("Something went wrong when trying to kick off a sync with Sync Monkey.");
        }
    }

    /**
     * Posts a file to an Rest API as long as current preferences (i.e. VPN and WiFi) allow it.
     *
     * @param fileToUpload The Uri of the file to upload
     * @since 0.2.0
     */
    public static void uploadFileToRemoteEndpoint(Uri fileToUpload)
    {
        Context context = getContext();
        final boolean transmitOnlyOnVpn = PreferenceUtils.getVpnOnlyPreference(context);
        final boolean transmitOnlyOnWiFi = PreferenceUtils.getWifiOnlyPreference(context);

        if (transmitOnlyOnWiFi && !isWiFiConnected())
        {
            String message = "Skipping upload because Wi-Fi Only setting is enabled and no Wi-Fi is available";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Timber.w(message);
            return;
        }

        if (transmitOnlyOnVpn)
        {
            if (isVpnEnabled())
            {
                uploadFiles(fileToUpload);
            } else
            {
                String message = "Skipping upload because VPN Only setting is enabled and no VPN is available";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                Timber.w(message);
            }
        } else
        {
            uploadFiles(fileToUpload);
        }
    }

    /**
     * Submit file to the submission handler for uploading
     * @param fileToUpload
     * @since 0.2.0
     */
    private static void uploadFiles(Uri fileToUpload)
    {
        try (Cursor cursor = getContext().getContentResolver().query(fileToUpload, null, null, null, null))
        {
            if (cursor != null && cursor.moveToFirst())
            {
                String filename = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                PhotoUploadSubmissionHandler.uploadPhotoToRemoteEndpoint(getContext(), filename);
            }
        }
    }

    /**
     * Check if the Android device is currently connected to a Wi-Fi network.
     *
     * @return If the device is connected to a Wi-Fi Network return true.
     */
    private static boolean isWiFiConnected()
    {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        boolean wifiConnected = false;

        for (Network network : connectivityManager.getAllNetworks())
        {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo == null) continue;

            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
            {
                wifiConnected = networkInfo.isConnected();
                break;
            }
        }

        return wifiConnected;
    }

    /**
     * Check if the Android device is currently attached to a VPN.
     *
     * @return If the device is connected to a VPN return true.
     */
    private static boolean isVpnEnabled()
    {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        final Network[] networks = connectivityManager.getAllNetworks();

        boolean vpnEnabled = false;

        for (Network network : networks)
        {
            final NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            if (caps == null) continue;

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))
            {
                vpnEnabled = true;
            }
        }

        return vpnEnabled;
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
