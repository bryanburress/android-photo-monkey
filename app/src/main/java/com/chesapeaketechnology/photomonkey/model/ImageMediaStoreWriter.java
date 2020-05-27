package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class ImageMediaStoreWriter extends ImageWriter {
    private static final String TAG = ImageMediaStoreWriter.class.getSimpleName();

    private final FileNameGenerator fileNameGenerator;

    public ImageMediaStoreWriter(FileNameGenerator fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    @Override
    public Uri write(ImageProxy image) throws FormatNotSupportedException, WriteException {

        // TODO: 5/26/20 Move off of the ui thread
        if(image.getFormat() == ImageFormat.JPEG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
                ContentValues contentValues = new ContentValues();

                // FIXME: 5/26/20 figure out what to do with these
                String title = "APicture";

                contentValues.put(MediaStore.MediaColumns.TITLE, title);
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameGenerator.generate().getName());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

//            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                Uri imageUri = resolver.insert(collection, contentValues);
                if (imageUri != null) {
                    try (OutputStream out = resolver.openOutputStream(imageUri)) {
                        byte[] data = ImageWriter.bytesForJpg(image);
                        if (out != null) {
                            out.write(data);
                        }

                        return imageUri;
                    } catch (FileNotFoundException e) {
                        throw new WriteException(String.format("Unable to access file uri '%s'", imageUri.toString()));
                    } catch (IOException e) {
                        throw new WriteException(String.format("Error accessing file '%s'", imageUri.toString()));
                    }
                } else {
                    throw new WriteException("Unable to retrieve uri for MediaStore.");
                }
            } else {
                throw new WriteException("Unsupported Android version.  Must be Q or greater.");
            }
        } else {
            throw new FormatNotSupportedException(String.format("%d is not a supported format.", image.getFormat()));
        }
    }
}
