package com.chesapeaketechnology.photomonkey;

import android.app.Activity;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static android.os.Environment.*;

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
    private View dialogView;
    private EditText description;
    private Button editButton;
    private Button newPhotoButton;

    private String currentPhotoPath;
    private Uri photoUri;
    private File imageFileChangeMe;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final Button saveButton = findViewById(R.id.saveButton);

        imageView = findViewById(R.id.imageView);
        dialogView = findViewById(R.id.dialog);
        description = findViewById(R.id.descriptionEditText);
        editButton = findViewById(R.id.editButton);
        newPhotoButton = findViewById(R.id.newPhotoButton);

        editButton.setOnClickListener(v -> {
            dialogView.setVisibility(View.VISIBLE);
            description.requestFocus();
        });

        newPhotoButton.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        saveButton.setOnClickListener(v -> {
            dialogView.setVisibility(View.GONE);
            newPhotoButton.setVisibility(View.VISIBLE);
            hideKeyboard();

            addExifDataToPhoto(description.getText().toString());
            addPhotoToGallery();
        });

        dispatchTakePictureIntent();
    }

    /**
     * Reset UI after taking a picture
     */
    private void resetUI()
    {
        newPhotoButton.setVisibility(View.GONE);
        dialogView.setVisibility(View.VISIBLE);
        description.setText("");
        description.requestFocus();
    }

    /**
     * Hides the keyboard if it is open.
     */
    private void hideKeyboard()
    {
        final InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
        {
            inputMethodManager.hideSoftInputFromWindow(dialogView.getWindowToken(), 0);
        }
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
            resetUI();

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

        // TODO: getExternalStorageDirectory is deprecated
        final File storageDir = new File(
                getExternalStorageDirectory(),
                PhotoMonkeyConstants.PHOTO_MONKEY_PICTURES_DIRECTORY
        );
//        final File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // TODO We might want to update this to the regular public photo directory

        storageDir.mkdir();
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFileChangeMe = image;

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void addExifDataToPhoto(String description)
    {
        try
        {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description);
            exif.saveAttributes();
        } catch (IOException e)
        {
            Log.e(LOG_TAG, "Error adding Exif data to image:");
            e.printStackTrace();
        }
    }

    /**
     * We want the picture to show up in the Android Gallery so that other apps can display this photo.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void addPhotoToGallery()
    {
        if (currentPhotoPath == null)
        {
            Log.wtf(LOG_TAG, "Could not send the photo to the Android Gallery because the currentPhotoPath is null");
            return;
        }

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(photoUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
