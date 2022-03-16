package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.camera.core.ImageProxy;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyFeatures;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import timber.log.Timber;

/**
 * The Image class manages the attributes associated with an image, exposes the necessary
 * functionality for operating on images, and encapsulates any differences between
 * the {@link MediaStore} and external media store {@link Context#getExternalMediaDirs()}.
 *
 * @since 0.2.0
 */
public class Image
{
    private final AMetadataDelegate metadataDelegate;
    private final PublicationDelegate publicationDelegate;
    private Uri uri;
    private Metadata metadata;

    private Image(Uri uri)
    {
        this.uri = uri;
        metadataDelegate = AMetadataDelegate.defaultMetadataDelegate();
        publicationDelegate = new PublicationDelegate();
    }

    private Image(Uri uri, AMetadataDelegate metadataDelegate)
    {
        this.uri = uri;
        this.metadataDelegate = metadataDelegate;
        publicationDelegate = new PublicationDelegate();
    }

    /**
     * Create an {@link Image} object from an {@link ImageProxy}. Uses the
     * default {@link AImageWriter} and {@link AMetadataDelegate} for the configured
     * media store ({@link PhotoMonkeyFeatures#USE_EXTERNAL_MEDIA_DIR}) to persist the
     * image and save the associated {@link Metadata}.
     *
     * @param image The ImageProxy object
     * @return the Image object created
     * @throws AImageWriter.FormatNotSupportedException If the image parameter is not a JPG.
     * @throws AImageWriter.WriteException              If we are unable to write metadata to the image.
     * @throws AMetadataDelegate.ReadFailure            If we are unable to read metadata from the image.
     */
    public static Image create(ImageProxy image)
            throws AImageWriter.FormatNotSupportedException, AImageWriter.WriteException,
            AMetadataDelegate.ReadFailure
    {
        AImageWriter writer;
        AMetadataDelegate mdd;
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR)
        {
            writer = new ImageFileWriter(new FileNameGenerator());
            mdd = AMetadataDelegate.defaultMetadataDelegate();
        } else
        {
            writer = new ImageMediaStoreWriter(new FileNameGenerator());
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        return create(image, writer, mdd);
    }

    /**
     * Create an {@link Image} object from a {@link Uri} for an image that already exists
     * in storage. Uses the default {@link AImageWriter} and {@link AMetadataDelegate} for
     * the configured media store ({@link PhotoMonkeyFeatures#USE_EXTERNAL_MEDIA_DIR}) to populate
     * the object.
     *
     * @param imageUri the location of the image
     * @return an Image object
     * @throws AMetadataDelegate.ReadFailure If we are unable to read metadata from the image.
     */
    public static Image create(Uri imageUri) throws AMetadataDelegate.ReadFailure
    {
        AMetadataDelegate mdd;
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR)
        {
            mdd = AMetadataDelegate.defaultMetadataDelegate();
        } else
        {
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        Image img = new Image(imageUri, mdd);
        Metadata metadata = mdd.read(img);
        img.setMetadata(metadata);
        return img;
    }

    /**
     * Create an {@link Image} object from an {@link ImageProxy}. Uses the
     * provided {@link AImageWriter} and {@link AMetadataDelegate} to persist the
     * image and save the associated {@link Metadata}.
     *
     * @param image            the actual image
     * @param imageWriter      the ImageWriter to be used for persisting the image
     * @param metadataDelegate the delegate used to read or modify the image's metatdata
     * @return an Image object
     * @throws AImageWriter.FormatNotSupportedException If the image parameter is not a JPG.
     * @throws AImageWriter.WriteException              If we are unable to write metadata to the image.
     * @throws AMetadataDelegate.ReadFailure            If we are unable to read metadata from the image.
     */
    public static Image create(ImageProxy image, AImageWriter imageWriter, AMetadataDelegate metadataDelegate)
            throws AImageWriter.FormatNotSupportedException, AImageWriter.WriteException,
            AMetadataDelegate.ReadFailure
    {
        Uri uri = imageWriter.write(image);
        Image img = new Image(uri, metadataDelegate);
        Metadata metadata = metadataDelegate.read(img);
        img.setMetadata(metadata);
        return img;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    private void setMetadata(Metadata metadata)
    {
        this.metadata = metadata;
    }

    /**
     * Get a {@link File} object representing the image
     *
     * @return File
     */
    public File getFile()
    {
        return new File(Objects.requireNonNull(uri.getPath()));
    }

    /**
     * Save metadata changes to the image.
     *
     * @param metadata The {@link Metadata} object to save
     * @return the updated Image
     * @throws AMetadataDelegate.SaveFailure if we are unable to save the metadata
     */
    public Image updateMetadata(Metadata metadata) throws AMetadataDelegate.SaveFailure
    {
        metadataDelegate.save(metadata, this);
        setMetadata(metadata);
        return this;
    }

    /**
     * Get {@link File} handle to a file that we can access and/or manipulate.  If this is an
     * image in the MediaStore, a the file will be copied to the external cache directory and
     * the returned {@link File} will point to that temporary file.
     *
     * @param context The {@link Context} in which to access the {@link android.provider.MediaStore}
     * @return a {@link File} object
     * @throws IOException if we are unable to write the temporary file.
     */
    public File getAccessibleFile(Context context) throws IOException
    {
        if ("content".equals(getUri().getScheme()))
        {
            // If this is an internal content url, we will need to copy the file to
            // the external cache folder in order for other applications to access it.
            ContentResolver resolver = context.getContentResolver();
            try (InputStream in = resolver.openInputStream(getUri()))
            {
                File outputDir = context.getExternalCacheDir();
                File tempFile = File.createTempFile("tmp_", ".jpg", outputDir);
                Timber.d("writeToTempFile: %s", tempFile.getAbsolutePath());
                java.nio.file.Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // This is not guaranteed to work. Have also created a reaper
                // in the activity onDestroy to clean up stragglers.
                tempFile.deleteOnExit();
                return tempFile;
            }
        } else
        {
            return getFile();
        }
    }

    /**
     * Get the {@link Uri} for the image.
     *
     * @return the {@link Uri}
     */
    public Uri getUri()
    {
        return uri;
    }

    /**
     * Set the {@link Uri} for the image.
     *
     * @param uri the image {@link Uri}.
     */
    public void setUri(Uri uri)
    {
        this.uri = uri;
    }

    /**
     * Publish, or share, the image with other applications on the system via a {@link PublicationDelegate}.
     *
     * @throws PublicationDelegate.PublicationFailure if there was an error publishing the image.
     */
    public void publish() throws PublicationDelegate.PublicationFailure
    {
        if (PhotoMonkeyFeatures.USE_EXTERNAL_MEDIA_DIR)
        {
            publicationDelegate.makeAvailableToOtherApps(this);
        }

        if (PhotoMonkeyFeatures.AUTOMATIC_SEND_TO_SYNC_MONKEY)
        {
            publicationDelegate.sendToSyncMonkey(this);
        }
    }
}
