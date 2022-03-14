package com.chesapeaketechnology.photomonkey.service;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.chesapeaketechnology.photomonkey.util.PhotoUploadApiUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;


/**
 * Responsible for executing the POST of a Photo object to the Rest API
 *
 * @since 0.2.0
 */
public class PhotoUploadWorker extends Worker
{
    public static final String PHOTO_PATH_KEY = "Photo-Path";
    public static final String DEVICE_ID_KEY = "Device-ID";
    public static final String PHOTOMONKEY_PHOTO_TAG = "PhotoMonkey-Photo";

    private final PhotoUploadService uploadService;

    public PhotoUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
    {
        super(context, workerParams);
        uploadService = PhotoUploadApiUtils.getRetrofitInstance(context).create(PhotoUploadService.class);
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Timber.i("Running the Photo Upload worker");

        final File photoFile = new File(Objects.requireNonNull(getInputData().getString(PHOTO_PATH_KEY)));
        final String deviceID = getInputData().getString(DEVICE_ID_KEY);
        byte[] photoContent;
        try
        {
            photoContent = Files.readAllBytes(photoFile.toPath());
        } catch (IOException e)
        {
            Timber.e("Failed to read photo content");
            return Result.failure();
        }

        final Call<ResponseBody> call = uploadService.postPhoto(new Photo(
                photoFile.getName(),
                Base64.encodeToString(photoContent, Base64.NO_WRAP),
                deviceID));
        try
        {
            final Response<ResponseBody> response = call.execute();

            if (response.isSuccessful())
            {
                Timber.i("Photo uploaded to Azure. Response: %s", response);
                return Result.success();
            } else
            {
                Timber.w("Upload failed, retrying. Response: %s", response);
                return Result.retry();
            }
        } catch (Throwable e)
        {
            Timber.e("Exception caught while uploading photo to Azure");
            return Result.retry();
        }
    }
}
