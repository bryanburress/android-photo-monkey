package com.chesapeaketechnology.photomonkey.model;

import android.graphics.ImageFormat;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.SINGLE_FILE_IO_TIMEOUT;

public class ImageFileWriter implements ImageWriter {
    private static final String TAG = ImageFileWriter.class.getSimpleName();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final File toFile;

    public ImageFileWriter(File toFile){
        this.toFile = toFile;
    }

    /**
     * Write {@link ImageProxy} to the specified file.
     * Currently, only supports {@link ImageFormat#JPEG}
     * @param image the {@link ImageProxy} to write
     *
     */
    @Override
    public void write(ImageProxy image) throws WriteException, FormatNotSupportedException {
        if (image.getFormat() == ImageFormat.JPEG) {
            try {
                Callable<Void> writeTask = () -> {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    // Rewind to make sure it is at the beginning of the buffer
                    buffer.rewind();

                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    FileOutputStream output = null;
                    try {
                        output = new FileOutputStream(toFile);
                        output.write(data);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing image data to file", e);
                    } finally {
                        image.close();
                        if (null != output) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error writing closing image data file", e);
                            }
                        }
                    }
                    return null;
                };

                Future<Void> result = executorService.submit(writeTask);
                result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);

            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new WriteException("Unable to save image", e.getCause());
            }
        } else {
            throw new FormatNotSupportedException(String.format("Format [%d] is not supported.", image.getFormat()));
        }

    }

    public static class FormatNotSupportedException extends Exception {
        public FormatNotSupportedException(String message) {
            super(message);
        }
    }

    public static class WriteException extends Exception {
        public WriteException(String message) {
            super(message);
        }

        public WriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
