package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyConfig;
import com.google.common.base.Throwables;
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

public class GalleryManager {
    private static final String TAG = GalleryManager.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public GalleryManager() {
    }

    public boolean isEmpty() throws GalleryAccessFailure{
        List<Uri> mediaList = getMedia();
        return (mediaList == null || mediaList.isEmpty());
    }

    public Uri getLatest() throws GalleryAccessFailure{
        try {
            Callable<List<Uri>> backgroundTask = () -> {
                return getMediaUris();
            };
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            List<Uri> mediaList = result.get(MULTI_FILE_IO_TIMEOUT, TimeUnit.SECONDS);
            if (mediaList != null && !mediaList.isEmpty()) {
                Uri lastUri = mediaList.get(mediaList.size() -1);
                return lastUri;
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new GalleryAccessFailure("Unable to get latest image from gallery", e);
        }
        return null;
    }

    public List<Uri> getMedia() throws GalleryAccessFailure {
        try {
            Callable<List<Uri>> backgroundTask = () -> {
                return getMediaUris();
            };
            Future<List<Uri>> result = executorService.submit(backgroundTask);
            List<Uri> mediaList = result.get(MULTI_FILE_IO_TIMEOUT, TimeUnit.SECONDS);
            return mediaList;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new GalleryAccessFailure("Unable to get latest image from gallery", e);
        }
    }


    private  List<Uri> getMediaUris(){
        if(PhotoMonkeyConfig.USE_EXTERNAL_MEDIA_DIR) {
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
            while(!cursor.isAfterLast()){
                Log.d(TAG, " - _ID : " + cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
                Log.d(TAG, " - File Name : " + cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Log.d(TAG, " - File Path : " + path);
                uris.add(Uri.parse(path));
                cursor.moveToNext();
            }
            cursor.close();
            return uris.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
    }

    public boolean delete(Uri mediaUri) throws GalleryDeleteFailure {
        Context context = PhotoMonkeyApplication.getContext();
        if(PhotoMonkeyConfig.USE_EXTERNAL_MEDIA_DIR) {
            File mediaFile = new File(mediaUri.getPath());
            boolean deleted = mediaFile.delete();
            if (!deleted) {
                throw new GalleryDeleteFailure("Unable to delete photo.");
            }
            MediaScannerConnection.scanFile(context, new String[]{mediaFile.getAbsolutePath()}, null, null);
        } else {
            ContentResolver resolver = context.getContentResolver();
            int rowsDeleted = resolver.delete(mediaUri, null, null);
            if (rowsDeleted < 1){
                throw new GalleryDeleteFailure("No rows were deleted.");
            }
        }
        return true;
    }

    public class GalleryAccessFailure extends Exception {
        public GalleryAccessFailure(String message) {
            super(message);
        }

        public GalleryAccessFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public class GalleryDeleteFailure extends Exception {
        public GalleryDeleteFailure(String message) {
            super(message);
        }

        public GalleryDeleteFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
