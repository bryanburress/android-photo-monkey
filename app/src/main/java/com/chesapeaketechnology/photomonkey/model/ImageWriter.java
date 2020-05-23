package com.chesapeaketechnology.photomonkey.model;

import androidx.camera.core.ImageProxy;

interface ImageWriter {
    void write(ImageProxy image) throws ImageFileWriter.FormatNotSupportedException, ImageFileWriter.WriteException;
}
