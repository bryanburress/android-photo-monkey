package com.chesapeaketechnology.photomonkey.model;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

/**
 * Provides functionality for EXIF data access and manipulation for image files
 * in the External Media Dir ({@link Context#getExternalMediaDirs()})
 *
 * @since 0.2.0
 */
public class ExifMetadataDelegate extends AMetadataDelegate
{
    private static final String TAG = ExifMetadataDelegate.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void save(Metadata metadata, Image forImage) throws SaveFailure
    {
        try
        {
            Callable<Void> backgroundTask = () -> {
                ExifInterface exif = new ExifInterface(forImage.getFile().getAbsolutePath());
                writeData(metadata, exif);
                Log.d(TAG, String.format("Saved image with supplementary data [%s]", metadata));
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
                ExifInterface exif = new ExifInterface(fromImage.getFile().getAbsolutePath());
                return readData(exif);
            };

            Future<Metadata> result = executorService.submit(backgroundTask);
            return result.get(SINGLE_FILE_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e)
        {
            throw new ReadFailure("Error accessing EXIF data.", e.getCause());
        }
    }

    protected Metadata readData(ExifInterface exif)
    {
        boolean reversed = exif.isFlipped();
        String provider = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
        Location location = new Location(provider);
        double[] latlong = exif.getLatLong();
        if (latlong != null)
        {
            location.setLatitude(latlong[0]);
            location.setLongitude(latlong[1]);
        }
        location.setAltitude(exif.getAltitude(0.0));
        double speed = exif.getAttributeDouble(ExifInterface.TAG_GPS_SPEED, 0.0);
        if (speed == 0)
        {
            location.setSpeed(0.0f);
        } else
        {
            location.setSpeed((float) (speed * 1000 / TimeUnit.HOURS.toSeconds(1)));
        }
        String description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
        return new Metadata(description, location, reversed);
    }

    protected void writeData(Metadata metadata, ExifInterface exif) throws IOException
    {
        if (metadata.isReversed())
        {
            exif.flipHorizontally();
        }
        if (metadata.getLocation() != null)
        {
            exif.setGpsInfo(metadata.getLocation());
        }
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, metadata.getDescription());
        exif.saveAttributes();
    }
}
