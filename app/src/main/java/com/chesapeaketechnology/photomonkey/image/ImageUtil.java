package com.chesapeaketechnology.photomonkey.image;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class ImageUtil {
    public static byte[] jpegImageToJpegByteArray(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.rewind();
        buffer.get(data);
        return data;
    }


}
