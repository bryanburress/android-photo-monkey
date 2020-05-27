package com.chesapeaketechnology.photomonkey.model;

import android.net.Uri;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public abstract class ImageWriter {
    public static byte[] bytesForJpg(ImageProxy image){
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        return data;
    }

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
