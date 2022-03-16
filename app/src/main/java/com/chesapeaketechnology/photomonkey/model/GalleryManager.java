package com.chesapeaketechnology.photomonkey.model;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;

import com.chesapeaketechnology.photomonkey.BuildConfig;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyFeatures;
import com.chesapeaketechnology.photomonkey.R;
import com.google.common.io.Files;

import java.io.File;
import java.nio.file.Paths;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.*;

/**
 * Provides access and management of the media stored in the external media
 * directory {@link Context#getExternalMediaDirs()}  or in the {@link MediaStore}
 *
 * @since 0.2.0
 */
public class GalleryManager
{
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static String getMediaStoreRelativePath()
    {
        return Paths.get(Environment.DIRECTORY_PICTURES, "PhotoMonkey").toString();
    }

    public GalleryManager()
    {
    }

    /**
     * Checks the media storage to determine if there are any images.
     *
     * @return true if there are no images in the gallery.
     * @throws GalleryAccessFailure if we were unable to access the gallery.
     */
    public boolean isEmpty() throws GalleryAccessFailure
    {
        List<Uri> mediaList = getMedia();
        return (mediaList == null || mediaList.isEmpty());
    }

    /**
     * Asynchronously, on a separate thread, get the {@link Uri} for the latest image in the gallery
     *
     * @return {@link Uri}
     * @throws GalleryAccessFailure if unable to access the images in the gallery
     */
    public Uri getLatest() throws GalleryAccessFailure
    {
        try
        {
            Callable<List<Uri>> backgroundTask = this::getMediaUris;
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            List<Uri> mediaList = result.get(MULTI_FILE_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (mediaList != null && !mediaList.isEmpty())
            {
                return mediaList.get(0);
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e)
        {
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
    public List<Uri> getMedia() throws GalleryAccessFailure
    {
        try
        {
            Callable<List<Uri>> backgroundTask = this::getMediaUris;
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            return result.get(MULTI_FILE_IO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e)
        {
            throw new GalleryAccessFailure("Unable to get latest image from gallery", e);
        }
    }

    /**
     * Logic for accessing the media and pivoting between external
     * media dir and MediaStore configurations.
     *
     * @return a list of image Uris.
     */
    private List<Uri> getMediaUris()
    {
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR)
        {
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
            return files.stream().sorted(Comparator.reverseOrder()).map(Uri::fromFile).collect(Collectors.toList());
        } else
        {
            List<Uri> uris = new ArrayList<>();
            ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
            final String sortOrder;
            final String selectionClause;
            final String[] selectionArgs;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            {
                // Sort the images from most recent to least recent
                sortOrder = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC";

                // Filter the images to those take with this application.
                selectionClause = MediaStore.Images.ImageColumns.OWNER_PACKAGE_NAME + " = ?";
                selectionArgs = new String[]{BuildConfig.APPLICATION_ID};
            } else
            {
                throw new RuntimeException("Unsupported Android version.  Must be Q or greater to use MediaStore storage.  Consider configuring for External Media Storage.");
            }
            try (Cursor cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, selectionClause, selectionArgs, sortOrder))
            {
                cursor.moveToFirst();
                while (!cursor.isAfterLast())
                {
                    @SuppressLint("Range")
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
                    uris.add(contentUri);
                    cursor.moveToNext();
                }
            }
            return uris.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
    }

    /**
     * Wraps delete method with a confirmation dialog.
     *
     * @param context            The UI context in which to display the dialog.
     * @param mediaUri           The {@link Uri} of the media to delete
     * @param discardDidComplete A {@link Consumer} that is called when the media is successfully deleted.
     * @param discardDidCancel   A {@link Consumer} that is called when the delete operation is cancelled by the user.
     * @param discardDidError    A {@link Consumer} that is called when there is an error deleting the media.
     */
    public void discard(Context context,
                        Uri mediaUri,
                        Consumer<Uri> discardDidComplete,
                        Consumer<Uri> discardDidCancel,
                        Consumer<Exception> discardDidError)
    {
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle("Confirm")
                .setMessage("Delete current photo?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.delete_button_alt, (_dialog, which) -> {
                    try
                    {
                        delete(mediaUri);
                        if (discardDidComplete != null)
                        {
                            discardDidComplete.accept(mediaUri);
                        }
                    } catch (GalleryDeleteFailure galleryDeleteFailure)
                    {
                        if (discardDidError != null)
                        {
                            discardDidError.accept(galleryDeleteFailure);
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog1, which) -> {
                    if (discardDidCancel != null)
                    {
                        discardDidCancel.accept(mediaUri);
                    }
                })
                .create();
        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.getDecorView().setSystemUiVisibility(FLAGS_FULLSCREEN);
        }
        dialog.show();
        if (window != null)
        {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

    /**
     * Remove a specific image from the external media dir or the MediaStore.
     *
     * @param mediaUri the uri for the image to remove
     * @return true if the delete was successful.
     * @throws GalleryDeleteFailure If we were unable to delete the image.
     */
    public boolean delete(Uri mediaUri) throws GalleryDeleteFailure
    {
        Context context = PhotoMonkeyApplication.getContext();
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR || mediaUri.getScheme() == null)
        {
            File mediaFile = new File(mediaUri.getPath());
            boolean deleted = mediaFile.delete();
            if (!deleted)
            {
                throw new GalleryDeleteFailure("Unable to delete photo.");
            }
            MediaScannerConnection.scanFile(context, new String[]{mediaFile.getAbsolutePath()}, null, null);
        } else
        {
            ContentResolver resolver = context.getContentResolver();
            int rowsDeleted = resolver.delete(mediaUri, null, null);
            if (rowsDeleted < 1)
            {
                throw new GalleryDeleteFailure("No rows were deleted.");
            }
        }
        return true;
    }

    /**
     * Indicates there was an issue accessing images in the gallery.
     */
    public static class GalleryAccessFailure extends Exception
    {
        public GalleryAccessFailure(String message)
        {
            super(message);
        }

        public GalleryAccessFailure(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Indicates there was an issue removing images from the gallery.
     */
    public static class GalleryDeleteFailure extends Exception
    {
        public GalleryDeleteFailure(String message)
        {
            super(message);
        }

        public GalleryDeleteFailure(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
