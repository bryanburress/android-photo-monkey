package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyFeatures;
import com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.EXTENSION_WHITELIST;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.MULTI_FILE_IO_TIMEOUT;

/**
 * Provides access and management of the media stored in the external media
 * directory {@link Context#getExternalMediaDirs()}  or in the {@link }MediaStore}
 *
 * @since 0.2.0
 */
public class GalleryManager {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public GalleryManager() {
    }

    /**
     * Checks the media storage to determine if there are any images.
     *
     * @return true if there are no images in the gallery.
     * @throws GalleryAccessFailure if we were unable to access the gallery.
     */
    public boolean isEmpty() throws GalleryAccessFailure {
        List<Uri> mediaList = getMedia();
        return (mediaList == null || mediaList.isEmpty());
    }

    /**
     * Asynchronously, on a separate thread, get the {@link Uri} for the latest image in the gallery
     *
     * @return {@link Uri}
     * @throws GalleryAccessFailure if unable to access the images in the gallery
     */
    public Uri getLatest() throws GalleryAccessFailure {
        try {
            Callable<List<Uri>> backgroundTask = () -> {
                return getMediaUris();
            };
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            List<Uri> mediaList = result.get(MULTI_FILE_IO_TIMEOUT, TimeUnit.SECONDS);
            if (mediaList != null && !mediaList.isEmpty()) {
                return mediaList.get(mediaList.size() - 1);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new GalleryAccessFailure("Unable to get latest image from gallery", e);
        }
        return null;
    }

    /**
     * Asynchronously, on a separate thread, get a list of all of the images in the
     * gallery in descending chronological order.
     *
     * @return List of Uri objects.
     * @throws GalleryAccessFailure unable to access the media
     */
    public List<Uri> getMedia() throws GalleryAccessFailure {
        try {
            Callable<List<Uri>> backgroundTask = () -> {
                return getMediaUris();
            };
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            return result.get(MULTI_FILE_IO_TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new GalleryAccessFailure("Unable to get latest image from gallery", e);
        }
    }

    /**
     * Logic for accessing the media and pivoting between external
     * media dir and MediaStore configurations.
     *
     * @return a list of image Uris.
     */
    private List<Uri> getMediaUris() {
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR) {
            File rootDirectory = new FileNameGenerator().getRootDirectory();
            // Walk through all files in the root directory
            List<File> files = Arrays.asList(Objects.requireNonNull(
                    rootDirectory.listFiles((dir, name) -> {
                        //noinspection UnstableApiUsage
                        String extension = Files.getFileExtension(name);
                        return EXTENSION_WHITELIST.contains(extension.toUpperCase(Locale.ROOT));
                    })
                    )
            );
            // Reverse the order of the list to present the last photos first
            return files.stream().sorted(Comparator.reverseOrder()).map(f -> Uri.fromFile(f)).collect(Collectors.toList());
        } else {
            List<Uri> uris = new ArrayList<>();
            ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
            Cursor cursor = null;
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    cursor = resolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            null,
                            null,
                            null,
                            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
                } else {
                    cursor = resolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            null,
                            null,
                            null,
                            MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC");
                }
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    //                String real_path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
                    uris.add(contentUri);
                    cursor.moveToNext();
                }
            } finally {
                if(cursor != null)
                    cursor.close();
            }
            return uris.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
    }

    /**
     * Remove a specific image from the external media dir or the MediaStore.
     *
     * @param mediaUri the uri for the image to remove
     * @return true if the delete was successful.
     * @throws GalleryDeleteFailure If we were unable to delete the image.
     */
    public boolean delete(Uri mediaUri) throws GalleryDeleteFailure {
        Context context = PhotoMonkeyApplication.getContext();
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR || mediaUri.getScheme() == null) {
            File mediaFile = new File(mediaUri.getPath());
            boolean deleted = mediaFile.delete();
            if (!deleted) {
                throw new GalleryDeleteFailure("Unable to delete photo.");
            }
            MediaScannerConnection.scanFile(context, new String[]{mediaFile.getAbsolutePath()}, null, null);
        } else {
            ContentResolver resolver = context.getContentResolver();
            int rowsDeleted = resolver.delete(mediaUri, null, null);
            if (rowsDeleted < 1) {
                throw new GalleryDeleteFailure("No rows were deleted.");
            }
        }
        return true;
    }

    public static class GalleryAccessFailure extends Exception {
        public GalleryAccessFailure(String message) {
            super(message);
        }

        public GalleryAccessFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GalleryDeleteFailure extends Exception {
        public GalleryDeleteFailure(String message) {
            super(message);
        }

        public GalleryDeleteFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
