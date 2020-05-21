package com.chesapeaketechnology.photomonkey.image;

import android.os.AsyncTask;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageSaver extends AsyncTask {
    private static final String LOG_TAG = ImageSaver.class.getSimpleName();

    private final ImageProxy image;
    private final File photoFile;

    public ImageSaver(ImageProxy image, File photoFile) {
        this.image = image;
        this.photoFile = photoFile;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        // Rewind to make sure it is at the beginning of the buffer
        buffer.rewind();

        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(photoFile);
            output.write(data);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error writing image data to file", e);
        } finally {
            image.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error writing closing image data file", e);
                }
            }
        }
        return null;
    }
}
