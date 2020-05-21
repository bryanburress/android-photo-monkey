package com.chesapeaketechnology.photomonkey.image;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;



public class ImageCaptureCallbackListener extends ImageCapture.OnImageCapturedCallback implements ImageDataResultListener {
    private static final String LOG_TAG = ImageCaptureCallbackListener.class.getSimpleName();

    private final File photoFile;
    private final int lensFacing;
    private final AsyncImageDataProvider descriptionProvider;
    private final ArrayList<ImageSavedListener> listeners = new ArrayList<ImageSavedListener>();

    public ImageCaptureCallbackListener(File photoFile, int lensFacing, AsyncImageDataProvider descriptionProvider, ImageSavedListener listener) {
        this.photoFile = photoFile;
        this.lensFacing = lensFacing;
        this.descriptionProvider = descriptionProvider;
        addListener(listener);
    }

    public void addListener(ImageSavedListener listener){
        listeners.add(listener);
    }

    public void removeListener(ImageSavedListener listener){
        listeners.remove(listener);
    }

    private void afterSave(ImageProxy image){
        listeners.forEach(imageSavedListener -> {
            imageSavedListener.onSaved(photoFile);
        });
        image.close();
    }

    @Override
    public void onCaptureSuccess(@NonNull ImageProxy image) {
        try {
            // Save the image to private storage
            ImageSaver saver = new ImageSaver(image, this.photoFile);
            saver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            if(descriptionProvider != null) {
                descriptionProvider.requestDescription(this);
            }
            afterSave(image);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(LOG_TAG, "Unable to save image", e);
        }
    }

    @Override
    public void onError(@NonNull ImageCaptureException exception) {
        Log.e(LOG_TAG, "Error capturing image.", exception);
    }

    @Override
    public void onData(String description, Location location) {
        try {
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                exif.flipHorizontally();
            }
            if(location != null){
                exif.setGpsInfo(location);
            }
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
            exif.saveAttributes();
            Log.d(LOG_TAG, String.format("Saved image with description [%s]", description));
        } catch (IOException e) {
                Log.e(LOG_TAG, "Error accessing EXIF data.", e);
        }
    }
}
