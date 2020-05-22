package com.chesapeaketechnology.photomonkey.image;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;

import com.chesapeaketechnology.photomonkey.sdata.SupplementaryData;
import com.chesapeaketechnology.photomonkey.sdata.SupplementaryDataProvider;
import com.chesapeaketechnology.photomonkey.sdata.SupplementaryDataDelegate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;



public class ImageCaptureCompletionDelegate extends ImageCapture.OnImageCapturedCallback implements SupplementaryDataDelegate {
    private static final String LOG_TAG = ImageCaptureCompletionDelegate.class.getSimpleName();

    private final File photoFile;
    private final int lensFacing;
    private final SupplementaryDataProvider descriptionProvider;
    private final ArrayList<ImageSaveCompletionDelegate> listeners = new ArrayList<ImageSaveCompletionDelegate>();

    public ImageCaptureCompletionDelegate(File photoFile, int lensFacing, SupplementaryDataProvider descriptionProvider, ImageSaveCompletionDelegate listener) {
        this.photoFile = photoFile;
        this.lensFacing = lensFacing;
        this.descriptionProvider = descriptionProvider;
        addListener(listener);
    }

    public void addListener(ImageSaveCompletionDelegate listener){
        listeners.add(listener);
    }

    public void removeListener(ImageSaveCompletionDelegate listener){
        listeners.remove(listener);
    }

    private void afterSave(ImageProxy image){
        listeners.forEach(delegate -> {
            delegate.imageWasSaved(photoFile);
        });
        image.close();
    }

    @Override
    public void onCaptureSuccess(@NonNull ImageProxy image) {
        try {
            // TODO: 5/21/20 Add guard statements for ensuring it's a jpg
            // Save the image to private storage
            ImageSaver saver = new ImageSaver(image, this.photoFile);
            saver.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
            if(descriptionProvider != null) {
                descriptionProvider.requestData(this);
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
    public void dataFetched(SupplementaryData data) {
        try {
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                exif.flipHorizontally();
            }
            if(data.getLocation() != null){
                exif.setGpsInfo(data.getLocation());
            }
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, data.getDescription());
            exif.saveAttributes();
            Log.d(LOG_TAG, String.format("Saved image with supplementary data [%s]", String.valueOf(data)));
        } catch (IOException e) {
                Log.e(LOG_TAG, "Error accessing EXIF data.", e);
        }
    }
}
