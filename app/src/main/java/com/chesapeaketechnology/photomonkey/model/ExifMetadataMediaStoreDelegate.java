package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.SINGLE_FILE_IO_TIMEOUT;

/**
 * Provides functionality for  EXIF data access and manipulation for image files
 * in the stored in the directory defined by ({@link MediaStore#VOLUME_EXTERNAL_PRIMARY}
 * (likely the /Pictures folder).
 * @since 0.1.0
 */
public class ExifMetadataMediaStoreDelegate extends  ExifMetadataDelegate {
    private static final String TAG = ExifMetadataDelegate.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Save the provided metadata to the image.
     * @param metadata
     * @param forImage
     * @throws SaveFailure
     */
    @Override
    public void save(Metadata metadata, Image forImage) throws SaveFailure {
        try {
            Callable<Void> backgroundTask = () -> {
                Uri uri = forImage.getUri();
                ExifInterface exif;
                if("content".equals(uri.getScheme())) {
                    ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
                    try (InputStream in = resolver.openInputStream(uri)) {
                        File tempFile = writeToTempFile(in);
                        exif = new ExifInterface(tempFile.getAbsolutePath());
                        writeData(metadata, exif);
                        try (OutputStream out = resolver.openOutputStream(uri, "rw")) {
                            Files.copy(tempFile.toPath(), out);
                        }
                        tempFile.delete();
                    }
                } else {
                    super.save(metadata, forImage);
                }
                Log.d(TAG, String.format("Saved image with supplementary data [%s]", String.valueOf(metadata)));
                return null;
            };

            Future<Void> result = executorService.submit(backgroundTask);
            result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new SaveFailure("Error saving EXIF data.", e.getCause());
        }
    }

    /**
     * Read the EXIF data from the image into a Metadata object.
     * @param fromImage
     * @return
     * @throws ReadFailure
     */
    @Override
    public Metadata read(Image fromImage) throws ReadFailure {
        try {
            Callable<Metadata> backgroundTask = () -> {
                Uri uri = fromImage.getUri();
                ExifInterface exif;
                if("content".equals(uri.getScheme())) {
                    ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
                    try(InputStream in = resolver.openInputStream(uri)) {
                        exif = new ExifInterface(in);
                        return readData(exif);
                    }
                } else {
                    return super.read(fromImage);
                }
            };

            Future<Metadata> result = executorService.submit(backgroundTask);
            return result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new ReadFailure("Error accessing EXIF data.", e.getCause());
        }
    }

    private File writeToTempFile(InputStream in) throws IOException {
        File outputDir = PhotoMonkeyApplication.getContext().getExternalCacheDir();
        File tempFile = File.createTempFile("tmp_", ".jpg", outputDir);
        tempFile.deleteOnExit();
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }
}
