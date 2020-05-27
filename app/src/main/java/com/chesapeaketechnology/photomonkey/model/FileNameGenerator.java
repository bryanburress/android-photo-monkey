package com.chesapeaketechnology.photomonkey.model;

import android.content.Context;
import android.util.Log;

import com.chesapeaketechnology.photomonkey.PhotoMonkeyApplication;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides tools for generating new file names and finding the path where they should be
 * written or read.
 *
 * @since 0.1.0
 */
public class FileNameGenerator {
    public static final String PREFIX = "PM_";
    public static final String FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    public static final String VOLUME = "PhotoMonkey";
    public static final String EXTENSION = ".jpg";
    private static final String TAG = FileNameGenerator.class.getSimpleName();
    private final File outputDirectory;

    public FileNameGenerator() {
        outputDirectory = getOutputDirectory();
    }

    private File getOutputDirectory() {
        Context appContext = PhotoMonkeyApplication.getContext();
        File[] mediaDirs = appContext.getExternalMediaDirs();
        if (mediaDirs != null && mediaDirs.length > 0) {
            File mediaDir = Arrays.stream(mediaDirs).filter(Objects::nonNull).findFirst().orElse(null);
            if (mediaDir != null && mediaDir.exists()) {
                return mediaDir;
            }
        }
        return appContext.getFilesDir();
    }

    /**
     * Get the directory where images are stored.
     *
     * @return
     */
    public File getRootDirectory() {
        Path outputPath = Paths.get(String.valueOf(outputDirectory.toPath()), VOLUME);
        File outputFolder = outputPath.toFile();
        boolean outputFolderExists = outputFolder.exists();
        if (!outputFolderExists) {
            outputFolderExists = outputFolder.mkdirs();
            if (!outputFolderExists) {
                outputFolder = outputDirectory;
            }
        }
        return outputFolder;
    }

    /**
     * Generate a unique, timestamped, file name and a {@link File} object with the full path to the file.
     *
     * @return
     */
    public File generate() {
        // TODO: 5/27/20 Ask Christian whether there is a concern about multiple users colliding which file names. Should we add a device id?
        File outputFolder = getRootDirectory();
        File fileName = new File(outputFolder, PREFIX + new SimpleDateFormat(FILE_NAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + EXTENSION);
        Log.d(TAG, String.format("Generated path: %s", fileName.getAbsolutePath()));
        return fileName;
    }

}
