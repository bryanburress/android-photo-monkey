package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication.*;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

import timber.log.Timber;

/**
 * Provides functionality for  EXIF data access and manipulation for image files
 * in the stored in the directory defined by ({@link MediaStore#VOLUME_EXTERNAL_PRIMARY}
 * (likely the /Pictures folder).
 *
 * @since 0.2.0
 */
public class ExifMetadataMediaStoreDelegate extends ExifMetadataDelegate
{
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void save(Metadata metadata, Image forImage) throws SaveFailure
    {
        try
        {
            Callable<Void> backgroundTask = () -> {
                Uri uri = forImage.getUri();
                ExifInterface exif;
                if ("content".equals(uri.getScheme()))
                {
                    File tempFile = forImage.getAccessibleFile(getContext());
                    exif = new ExifInterface(tempFile.getAbsolutePath());
                    writeData(metadata, exif);
                    ContentResolver resolver = getContext().getContentResolver();
                    try (OutputStream out = resolver.openOutputStream(uri, "rw"))
                    {
                        Files.copy(tempFile.toPath(), Objects.requireNonNull(out));
                    }
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                } else
                {
                    super.save(metadata, forImage);
                }
                Timber.d("Saved image with supplementary data [%s]", metadata);
                return null;
            };

            Future<Void> result = executorService.submit(backgroundTask);
            result.get(SINGLE_FILE_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e)
        {
            throw new SaveFailure("Error saving EXIF data.", e.getCause());
        }
    }

    @Override
    public Metadata read(Image fromImage) throws ReadFailure
    {
        try
        {
            Callable<Metadata> backgroundTask = () -> {
                Uri uri = fromImage.getUri();
                ExifInterface exif;
                if ("content".equals(uri.getScheme()))
                {
                    ContentResolver resolver = getContext().getContentResolver();
                    try (InputStream in = resolver.openInputStream(uri))
                    {
                        exif = new ExifInterface(Objects.requireNonNull(in));
                        return readData(exif);
                    }
                } else
                {
                    return super.read(fromImage);
                }
            };

            Future<Metadata> result = executorService.submit(backgroundTask);
            return result.get(SINGLE_FILE_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e)
        {
            throw new ReadFailure("Error accessing EXIF data.", e.getCause());
        }
    }

    /**
     * Writes an {@link InputStream} to a temporary file in the {@link Context#getExternalCacheDir()} folder.
     *
     * @param in The {@link InputStream} to write into a temporary file.
     * @return A {@link File} object referencing the temporary file that was created.
     * @throws IOException if we were unable to copy the input stream to the temporary file
     */
    private File writeToTempFile(InputStream in) throws IOException
    {
        File outputDir = getContext().getExternalCacheDir();
        File tempFile = File.createTempFile("tmp_", ".jpg", outputDir);
        Timber.d("writeToTempFile: %s", tempFile.getAbsolutePath());
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }
}
