package com.chesapeaketechnology.photomonkey.model;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.camera.core.ImageProxy;
import androidx.core.content.FileProvider;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyActivity;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyConfig;
import com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants;
import com.google.common.io.Files;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// TODO: 5/23/20 Add support for public gallery, private gallery, and potentially a public folder. Will need a configuration constant to govern.
public class Image {
    private static final String TAG = Image.class.getSimpleName();

    public static Image create(ImageProxy image)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        ImageWriter writer;
        MetadataDelegate mdd;
        Context context = PhotoMonkeyApplication.getContext();
        if(PhotoMonkeyConfig.USE_EXTERNAL_MEDIA_DIR){
            writer = new ImageFileWriter(new FileNameGenerator());
            mdd = MetadataDelegate.defaultMetadataDelegate();
        } else {
            writer = new ImageMediaStoreWriter(new FileNameGenerator());
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        return create(image, writer, mdd);
    }

    public static Image create(Uri imageUri)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        MetadataDelegate mdd;
        if(PhotoMonkeyConfig.USE_EXTERNAL_MEDIA_DIR){
            mdd = MetadataDelegate.defaultMetadataDelegate();
        } else {
            mdd = new ExifMetadataMediaStoreDelegate();
        }
        Image img = new Image(imageUri, mdd);
        Metadata metadata = mdd.read(img);
        img.setMetadata(metadata);
        return img;
    }

    public static Image create(ImageProxy image, ImageWriter imageWriter)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        return create(image, imageWriter, MetadataDelegate.defaultMetadataDelegate());
    }

    public static Image create(ImageProxy image, ImageWriter imageWriter, MetadataDelegate metadataDelegate)
            throws ImageWriter.FormatNotSupportedException, ImageWriter.WriteException,
            MetadataDelegate.ReadFailure {
        Uri uri = imageWriter.write(image);
        Image img = new Image(uri, metadataDelegate);
        Metadata metadata = metadataDelegate.read(img);
        img.setMetadata(metadata);
        return img;
    }

    private final MetadataDelegate metadataDelegate;
    private final PublicationDelegate publicationDelegate;
    private Uri uri;
    private Metadata metadata;

    public Image(Uri uri){
        this.uri = uri;
        metadataDelegate = MetadataDelegate.defaultMetadataDelegate();
        publicationDelegate = new PublicationDelegate();
    }

    public Image(Uri uri, MetadataDelegate metadataDelegate){
        this.uri = uri;
        this.metadataDelegate = metadataDelegate;
        publicationDelegate = new PublicationDelegate();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    private void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public File getFile() {
        return new File(Objects.requireNonNull(uri.getPath()));
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

    public Uri getUri(){
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void publish() throws PublicationDelegate.PublicationFailure {
        if(PhotoMonkeyConfig.USE_EXTERNAL_MEDIA_DIR) {
            publicationDelegate.makeAvailableToOtherApps(this);
        }

        if(PhotoMonkeyConfig.AUTOMATIC_SEND_TO_SYNC_MONKEY) {
            publicationDelegate.sendToSyncMonkey(this);
        }
    }


}
