package com.chesapeaketechnology.photomonkey.model;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.camera.core.ImageProxy;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyFeatures;

import java.io.File;
import java.util.Objects;

/**
 * The Image class manages the attributes associated with an image, exposes the necessary
 * functionality for operating on images, and encapsulates any differences between
 * the {@link MediaStore} and external media store {@link Context#getExternalMediaDirs()}.
 *
 * @since 0.2.0
 */
public class Image {
    private final MetadataDelegate metadataDelegate;
    private final PublicationDelegate publicationDelegate;
    private Uri uri;
    private Metadata metadata;
    private Image(Uri uri) {
        this.uri = uri;
        metadataDelegate = MetadataDelegate.defaultMetadataDelegate();
        publicationDelegate = new PublicationDelegate();
    }
    private Image(Uri uri, MetadataDelegate metadataDelegate) {
        this.uri = uri;
        this.metadataDelegate = metadataDelegate;
        publicationDelegate = new PublicationDelegate();
    }

    /**
     * Create an {@link Image} object from an {@link ImageProxy}. Uses the
     * default {@link ImageWriter} and {@link MetadataDelegate} for the configured
     * media store ({@link PhotoMonkeyFeatures#USE_EXTERNAL_MEDIA_DIR}) to persist the
     * image and save the associated {@link Metadata}.
     *
     * @param image The ImageProxy object
     * @return
     * @throws ImageWriter.FormatNotSupportedException If the image parameter is not a JPG.
     * @throws ImageWriter.WriteException              If we are unable to write metadata to the image.
     * @throws MetadataDelegate.ReadFailure            If we are unable to read metadata from the image.
     */
    public static Image create(ImageProxy image)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        ImageWriter writer;
        MetadataDelegate mdd;
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR) {
            writer = new ImageFileWriter(new FileNameGenerator());
            mdd = MetadataDelegate.defaultMetadataDelegate();
        } else {
            writer = new ImageMediaStoreWriter(new FileNameGenerator());
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        return create(image, writer, mdd);
    }

    /**
     * Create an {@link Image} object from a {@link Uri} for an image that already exists
     * in storage. Uses the default {@link ImageWriter} and {@link MetadataDelegate} for
     * the configured media store ({@link PhotoMonkeyFeatures#USE_EXTERNAL_MEDIA_DIR}) to populate
     * the object.
     *
     * @param imageUri the location of the image
     * @return an Image object
     * @throws MetadataDelegate.ReadFailure            If we are unable to read metadata from the image.
     */
    public static Image create(Uri imageUri) throws MetadataDelegate.ReadFailure {
        MetadataDelegate mdd;
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR) {
            mdd = MetadataDelegate.defaultMetadataDelegate();
        } else {
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        Image img = new Image(imageUri, mdd);
        Metadata metadata = mdd.read(img);
        img.setMetadata(metadata);
        return img;
    }

    /**
     * Create an {@link Image} object from an {@link ImageProxy}. Uses the
     * provided {@link ImageWriter} and {@link MetadataDelegate} to persist the
     * image and save the associated {@link Metadata}.
     *
     * @param image            the actual image
     * @param imageWriter      the ImageWriter to be used for persisting the image
     * @param metadataDelegate the delegate used to read or modify the image's metatdata
     * @return an Image object
     * @throws ImageWriter.FormatNotSupportedException If the image parameter is not a JPG.
     * @throws ImageWriter.WriteException              If we are unable to write metadata to the image.
     * @throws MetadataDelegate.ReadFailure            If we are unable to read metadata from the image.
     */
    public static Image create(ImageProxy image, ImageWriter imageWriter, MetadataDelegate metadataDelegate)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        Uri uri = imageWriter.write(image);
        Image img = new Image(uri, metadataDelegate);
        Metadata metadata = metadataDelegate.read(img);
        img.setMetadata(metadata);
        return img;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    private void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Get a {@link File} object representing the image
     *
     * @return File
     */
    public File getFile() {
        return new File(Objects.requireNonNull(uri.getPath()));
    }

    /**
     * Save metadata changes to the image.
     *
     * @param metadata
     * @return the updated Image
     * @throws MetadataDelegate.SaveFailure if we are unable to save the metadata
     */
    public Image updateMetadata(Metadata metadata) throws MetadataDelegate.SaveFailure {
        metadataDelegate.save(metadata, this);
        setMetadata(metadata);
        return this;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    /**
     * Publish, or share, the image with other applications on the system.
     *
     * @throws PublicationDelegate.PublicationFailure
     */
    public void publish() throws PublicationDelegate.PublicationFailure {
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR) {
            publicationDelegate.makeAvailableToOtherApps(this);
        }

        if (PhotoMonkeyFeatures.AUTOMATIC_SEND_TO_SYNC_MONKEY) {
            publicationDelegate.sendToSyncMonkey(this);
        }
    }
}
