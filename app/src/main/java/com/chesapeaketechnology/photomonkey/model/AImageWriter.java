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
public abstract class AImageWriter
{
    /**
     * Get the byte[] for an {@link ImageProxy} jpg image.
     *
     * @param image The {@link ImageProxy} containing the image.
     * @return a byte[] representing the image.
     */
    public static byte[] bytesForJpg(ImageProxy image)
    {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        return data;
    }

    /**
     * Write {@link ImageProxy} to the appropriate storage.
     * Currently, only supports {@link ImageFormat#JPEG}
     *
     * @param image the {@link ImageProxy} to write
     * @return the {@link Uri} for the written file.
     */
    abstract Uri write(ImageProxy image) throws FormatNotSupportedException, WriteException;

    /**
     * Indicates that the format of the image is not currently supported.  Currently, only
     * JPG is supported.
     */
    public static class FormatNotSupportedException extends Exception
    {
        public FormatNotSupportedException(String message)
        {
            super(message);
        }
    }

    /**
     * Indicates there was an error writing the image or the image metadata.
     */
    public static class WriteException extends Exception
    {
        public WriteException(String message)
        {
            super(message);
        }

        public WriteException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
