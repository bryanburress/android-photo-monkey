package com.chesapeaketechnology.photomonkey.model;

import androidx.camera.core.ImageProxy;

import java.io.File;

// TODO: 5/23/20 Add support for public gallery, private gallery, and potentially a public folder. Will need a configuration constant to govern.
public class Image {
    private static final String TAG = Image.class.getSimpleName();

    public static Image create(ImageProxy image, File toFile)
            throws ImageFileWriter.FormatNotSupportedException, ImageFileWriter.WriteException,
            MetadataDelegate.ReadFailure {
        return Image.create(image, toFile, new ImageFileWriter(toFile));
    }

    public static Image create(ImageProxy image, File toFile, ImageWriter imageWriter)
            throws ImageFileWriter.FormatNotSupportedException, ImageFileWriter.WriteException,
            MetadataDelegate.ReadFailure {
        return Image.create(image, toFile, new ImageFileWriter(toFile), MetadataDelegate.defaultMetadataDelegate());
    }

    public static Image create(ImageProxy image, File toFile, ImageWriter imageWriter, MetadataDelegate metadataDelegate)
            throws ImageFileWriter.FormatNotSupportedException, ImageFileWriter.WriteException,
            MetadataDelegate.ReadFailure {
        Image _image = new Image(toFile, metadataDelegate);
        imageWriter.write(image);
        Metadata metadata = metadataDelegate.read(_image);
        _image.setMetadata(metadata);
        return _image;
    }

    private final MetadataDelegate metadataDelegate;
    private File file;
    private Metadata metadata;

    public Image(File file){
        this.file = file;
        this.metadataDelegate = MetadataDelegate.defaultMetadataDelegate();
    }

    public Image(File file, MetadataDelegate metadataDelegate){
        this.file = file;
        this.metadataDelegate = metadataDelegate;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    private void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public File getFile() {
        return file;
    }

    private void setFile(File file) {
        this.file = file;
    }

    public Image updateMetadata(Metadata metadata) throws MetadataDelegate.SaveFailure {
        metadataDelegate.save(metadata, this);
        setMetadata(metadata);
        return this;
    }

    public Image refreshMetadata() throws MetadataDelegate.ReadFailure {
        Metadata metadata = metadataDelegate.read(this);
        setMetadata(metadata);
        return this;
    }

    public void publish() {}

}
