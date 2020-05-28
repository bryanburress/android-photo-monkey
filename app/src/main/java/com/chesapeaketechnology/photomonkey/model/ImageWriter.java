package com.chesapeaketechnology.photomonkey.model;

import android.graphics.ImageFormat;
import android.net.Uri;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/**
 * Abstraction for writing image files based on different storage configurations.
 *
 * @since 0.2.0
 */
public abstract class ImageWriter {
    public static byte[] bytesForJpg(ImageProxy image){
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        return data;
    }

    /**
     * Write {@link ImageProxy} to the appropriate storage.
     * Currently, only supports {@link ImageFormat#JPEG}
     * @param image the {@link ImageProxy} to write
     *
     * @return the {@link Uri} for the written file.
     */
    abstract Uri write(ImageProxy image) throws FormatNotSupportedException, WriteException;

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
