package com.chesapeaketechnology.photomonkey;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.FLAGS_FULLSCREEN;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.IMMERSIVE_FLAG_TIMEOUT;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_ACTION;
import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.KEY_EVENT_EXTRA;


/**
 * The main activity for the Photo Monkey app.  This activity first launches the Android device's default camera app,
 * and once the user takes a picture they are shown the picture to add any details.
 *
 * @since 0.1.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity
{
    private static final String LOG_TAG = PhotoMonkeyActivity.class.getSimpleName();
    private FrameLayout container;

    public static File getOutputDirectory(Context context) {
        Context appContext = context.getApplicationContext();
        File[] mediaDirs = context.getExternalMediaDirs();
        if ( mediaDirs != null && mediaDirs.length > 0 ) {
            File mediaDir = Arrays.stream(mediaDirs).filter(Objects::nonNull).findFirst().orElse(null);
            if (mediaDir != null && mediaDir.exists()) {
                return mediaDir;
            }
        }
        return appContext.getFilesDir();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);
    }


    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                container.setSystemUiVisibility(FLAGS_FULLSCREEN);
            }
        }, IMMERSIVE_FLAG_TIMEOUT);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent = intent.putExtra(KEY_EVENT_EXTRA, keyCode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data)
//    {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
//        {
//            if (photoUri == null)
//            {
//                Log.wtf(LOG_TAG, "Could not add the photo to the image view because the photoUri is null");
//                return;
//            }
//
//            imageView.setImageURI(photoUri);
//
//            // TODO 1. Update the UI to add options for adding a description to the photo.
//            // TODO 2. Record the user description in the exif data of the photo
//            // TODO 3. Send the image file over to Sync Monkey so it can be uploaded to Azure, or maybe update Sync
//            //  Monkey so we can send it a command to sync now if we go the route of adding in the photo directory to
//            //  the list of directories to sync.
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    /**
     * Create a picture intent and send it to the default camera app.
     * <p>
     * Once the user takes the picture we will show it to the user so they can add a description to it.
     */
//    private void dispatchTakePictureIntent()
//    {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        // Ensure that there's a camera activity to handle the intent
//        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
//        {
//            // Create the File where the photo should go
//            File photoFile = null;
//            try
//            {
//                photoFile = createImageFile();
//            } catch (IOException e)
//            {
//                // Error occurred while creating the File
//                Log.e(LOG_TAG, "Could not create the image file for the Photo Monkey app", e);
//            }
//
//            // Continue only if the File was successfully created
//            if (photoFile != null)
//            {
//                photoUri = FileProvider.getUriForFile(this, PhotoMonkeyConstants.AUTHORITY, photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
//                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//            }
//        } else
//        {
//            // TODO Display an error (Snackbar) to the user that the phone does not have a supported camera app
//        }
//    }

//    /**
//     * Create a file with a unique file name where the photo can be stored.
//     *
//     * @return A File object where the photo can be stored.
//     * @throws IOException If something goes wrong wile trying to create the file.
//     */
//    private File createImageFile() throws IOException
//    {
//        // Create an image file name
//        final String timeStamp = DATE_TIME_FORMATTER.format(ZonedDateTime.now());
//        final String imageFileName = PhotoMonkeyConstants.PHOTO_MONKEY_PHOTO_NAME_PREFIX + timeStamp;
//        final File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // TODO We might want to update this to the regular public photo directory
//        final File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",   /* suffix */
//                storageDir      /* directory */
//        );
//
//        // Save a file: path for use with ACTION_VIEW intents
//        currentPhotoPath = image.getAbsolutePath();
//        return image;
//    }

//    /**
//     * We want the picture to show up in the Android Gallery so that other apps can display this photo.
//     */
//    private void addPhotoToGallery()
//    {
//        if (currentPhotoPath == null)
//        {
//            Log.wtf(LOG_TAG, "Could not send the photo to the Android Gallery because the currentPhotoPath is null");
//            return;
//        }
//
//        // FIXME I don't think this actually works as is.  If we switch to using the public photo directory we won't need to share it to the gallery at all
//
//        final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        final File photoFile = new File(currentPhotoPath);
//        final Uri contentUri = Uri.fromFile(photoFile);
//        mediaScanIntent.setData(contentUri);
//        sendBroadcast(mediaScanIntent);
//    }
}
