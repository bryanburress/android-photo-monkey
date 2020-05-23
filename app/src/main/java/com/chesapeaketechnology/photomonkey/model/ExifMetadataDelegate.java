package com.chesapeaketechnology.photomonkey.model;

import android.location.Location;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.SINGLE_FILE_IO_TIMEOUT;

public class ExifMetadataDelegate extends MetadataDelegate {
    private static final String TAG = ExifMetadataDelegate.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public ExifMetadataDelegate() {}

    @Override
    public void save(Metadata metadata, Image forImage) throws SaveFailure {
        try {
            Callable<Void> backgroundTask = () -> {
                ExifInterface exif = new ExifInterface(forImage.getFile().getAbsolutePath());
                if (metadata.isReversed()) {
                    exif.flipHorizontally();
                }
                if(metadata.getLocation() != null){
                    exif.setGpsInfo(metadata.getLocation());
                }
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, metadata.getDescription());
                exif.saveAttributes();
                Log.d(TAG, String.format("Saved image with supplementary data [%s]", String.valueOf(metadata)));
                return null;
            };

            Future<Void> result = executorService.submit(backgroundTask);
            result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new SaveFailure("Error saving EXIF data.", e.getCause());
        }
    }

    @Override
    public Metadata read(Image fromImage) throws ReadFailure {
        try {
            Callable<Metadata> backgroundTask = () -> {
                ExifInterface exif = new ExifInterface(fromImage.getFile().getAbsolutePath());
                boolean reversed = exif.isFlipped();
                String provider = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
                Location location = new Location(provider);
                double[] latlong = exif.getLatLong();
                location.setLatitude(latlong[0]);
                location.setLongitude(latlong[1]);
                location.setAltitude(exif.getAltitude(0.0));
                String speedStr = exif.getAttribute(ExifInterface.TAG_GPS_SPEED);
                if (speedStr != null) {
                    location.setSpeed(Float.parseFloat(speedStr) / (TimeUnit.HOURS.toSeconds(1) * 1000));
                }
                String description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
                return new Metadata(description, location, reversed);
            };

            Future<Metadata> result = executorService.submit(backgroundTask);
            return result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new ReadFailure("Error accessing EXIF data.", e.getCause());
        }
    }
}
