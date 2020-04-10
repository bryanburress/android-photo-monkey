package com.chesapeaketechnology.photomonkey;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The main activity for the Photo Monkey app.  This activity first launches the Android device's default camera app,
 * and once the user takes a picture they are shown the picture to add any details.
 *
 * @since 0.1.0
 */
public class PhotoMonkeyActivity extends AppCompatActivity
{
    private static final String LOG_TAG = PhotoMonkeyActivity.class.getSimpleName();

    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ImageView imageView;
    private String currentPhotoPath;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);

        dispatchTakePictureIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            if (photoUri == null)
            {
                Log.wtf(LOG_TAG, "Could not add the photo to the image view because the photoUri is null");
                return;
            }

            imageView.setImageURI(photoUri);

            // TODO 1. Update the UI to add options for adding a description to the photo.
            // TODO 2. Record the user description in the exif data of the photo
            // TODO 3. Send the image file over to Sync Monkey so it can be uploaded to Azure, or maybe update Sync
            //  Monkey so we can send it a command to sync now if we go the route of adding in the photo directory to
            //  the list of directories to sync.
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Create a picture intent and send it to the default camera app.
     * <p>
     * Once the user takes the picture we will show it to the user so they can add a description to it.
     */
    private void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            // Create the File where the photo should go
            File photoFile = null;
            try
            {
                photoFile = createImageFile();
            } catch (IOException e)
            {
                // Error occurred while creating the File
                Log.e(LOG_TAG, "Could not create the image file for the Photo Monkey app", e);
            }

            // Continue only if the File was successfully created
            if (photoFile != null)
            {
                photoUri = FileProvider.getUriForFile(this, PhotoMonkeyConstants.AUTHORITY, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else
        {
            // TODO Display an error (Snackbar) to the user that the phone does not have a supported camera app
        }
    }

    /**
     * Create a file with a unique file name where the photo can be stored.
     *
     * @return A File object where the photo can be stored.
     * @throws IOException If something goes wrong wile trying to create the file.
     */
    private File createImageFile() throws IOException
    {
        // Create an image file name
        final String timeStamp = DATE_TIME_FORMATTER.format(ZonedDateTime.now());
        final String imageFileName = PhotoMonkeyConstants.PHOTO_MONKEY_PHOTO_NAME_PREFIX + timeStamp;
        final File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // TODO We might want to update this to the regular public photo directory
        final File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * We want the picture to show up in the Android Gallery so that other apps can display this photo.
     */
    private void addPhotoToGallery()
    {
        if (currentPhotoPath == null)
        {
            Log.wtf(LOG_TAG, "Could not send the photo to the Android Gallery because the currentPhotoPath is null");
            return;
        }

        // FIXME I don't think this actually works as is.  If we switch to using the public photo directory we won't need to share it to the gallery at all

        final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final File photoFile = new File(currentPhotoPath);
        final Uri contentUri = Uri.fromFile(photoFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }
}
