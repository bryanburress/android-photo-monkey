package com.chesapeaketechnology.photomonkey.service;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.PROPERTY_DEVICE_ID_KEY;
import static com.chesapeaketechnology.photomonkey.service.PhotoUploadWorker.DEVICE_ID_KEY;
import static com.chesapeaketechnology.photomonkey.service.PhotoUploadWorker.PHOTOMONKEY_PHOTO_TAG;
import static com.chesapeaketechnology.photomonkey.service.PhotoUploadWorker.PHOTO_PATH_KEY;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.time.Duration;

import timber.log.Timber;

/**
 * Responsible for taking a photo and posting it to the Rest endpoint. If for whatever reason
 * the API is not available or the POST failed for a different reason, this class handles
 * either retrying the API call, or notifying the user about the problem.
 *
 * @since 0.2.0
 */
public class PhotoUploadSubmissionHandler
{
    /**
     * Send a report to the File Upload REST API endpoint.
     *
     * @param photoFilePath The photo to upload
     */
    public static void uploadPhotoToRemoteEndpoint(Context context, String photoFilePath)
    {
        String deviceId = PreferenceManager.getDefaultSharedPreferences(context).getString(PROPERTY_DEVICE_ID_KEY, "");
        if (deviceId.isEmpty())
        {
            Timber.e("Could not determine device ID. Skipping file upload");
            return;
        }

        final Data data = new Data.Builder()
                .putString(PHOTO_PATH_KEY, photoFilePath)
                .putString(DEVICE_ID_KEY, deviceId)
                .build();
        final WorkRequest photoUploadRequest = new OneTimeWorkRequest.Builder(PhotoUploadWorker.class)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(10))
                .addTag(PHOTOMONKEY_PHOTO_TAG)
                .setInputData(data)
                .build();

        WorkManager.getInstance(context).enqueue(photoUploadRequest);
    }
}
