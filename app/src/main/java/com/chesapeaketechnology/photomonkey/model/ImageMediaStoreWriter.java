package com.chesapeaketechnology.photomonkey.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.camera.core.ImageProxy;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.chesapeaketechnology.photomonkey.PhotoMonkeyConstants.SINGLE_FILE_IO_TIMEOUT;

/**
 * Write an {@link ImageProxy} to the {@link MediaStore} using the MediaStore APIs.
 *
 * @since 0.2.0
 */
public class ImageMediaStoreWriter extends ImageWriter {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final FileNameGenerator fileNameGenerator;

    public ImageMediaStoreWriter(FileNameGenerator fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    /**
     * Write {@link ImageProxy} to the MediaStore.
     * Currently, only supports {@link ImageFormat#JPEG}
     *
     * @param image the {@link ImageProxy} to write
     * @return the {@link Uri} for the written file.
     */
    @Override
    public Uri write(ImageProxy image) throws FormatNotSupportedException, WriteException {

        // TODO: 5/26/20 Move off of the ui thread
        if (image.getFormat() == ImageFormat.JPEG) {
            try {
                Callable<Uri> writeTask = () -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentResolver resolver = PhotoMonkeyApplication.getContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();

                        FileNameGenerator generator = new FileNameGenerator();
                        String title = generator.generate().getName();
                        String relativePath = Paths.get(Environment.DIRECTORY_PICTURES, "PhotoMonkey").toString();
                        contentValues.put(MediaStore.MediaColumns.TITLE, title);
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameGenerator.generate().getName());
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

                        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        Uri imageUri = resolver.insert(collection, contentValues);
                        if (imageUri != null) {
                            try (OutputStream out = resolver.openOutputStream(imageUri)) {
                                byte[] data = ImageWriter.bytesForJpg(image);
                                if (out != null) {
                                    out.write(data);
                                }
                                return imageUri;
                            } catch (FileNotFoundException e) {
                                throw new WriteException(String.format("Unable to access file uri '%s'", imageUri.toString()));
                            } catch (IOException e) {
                                throw new WriteException(String.format("Error accessing file '%s'", imageUri.toString()));
                            }
                        } else {
                            throw new WriteException("Unable to retrieve uri for MediaStore.");
                        }
                    } else {
                        throw new WriteException("Unsupported Android version.  Must be Q or greater.");
                    }
                };

                Future<Uri> result = executorService.submit(writeTask);
                return result.get(SINGLE_FILE_IO_TIMEOUT, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                throw new WriteException("Unable to save image", e.getCause());
            }
        } else {
            throw new FormatNotSupportedException(String.format("%d is not a supported format.", image.getFormat()));
        }
    }
}
