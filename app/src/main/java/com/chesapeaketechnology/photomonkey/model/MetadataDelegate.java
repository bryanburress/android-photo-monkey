package com.chesapeaketechnology.photomonkey.model;

public abstract class MetadataDelegate {

    public static MetadataDelegate defaultMetadataDelegate() {
        return new ExifMetadataDelegate();
    }

    /**
     * Support for future alternative storage mechanisms (database, sidecar, etc).
     *
     * @param strategy
     * @return
     */
    public static MetadataDelegate getDelegate(PersistenceStrategy strategy) {
        if (strategy == PersistenceStrategy.EXIF) {
            return new ExifMetadataDelegate();
        }
        return defaultMetadataDelegate();
    }

    /**
     * Save the provided metadata to the image.
     *
     * @param metadata
     * @param forImage
     * @throws SaveFailure
     */
    abstract void save(Metadata metadata, Image forImage) throws SaveFailure;

    /**
     * Read the EXIF data from the image into a Metadata object.
     *
     * @param fromImage
     * @return
     * @throws ReadFailure
     */
    abstract Metadata read(Image fromImage) throws ReadFailure;

    public enum PersistenceStrategy {
        EXIF
    }

    public static class SaveFailure extends Exception {
        public SaveFailure(String message) {
            super(message);
        }

        public SaveFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ReadFailure extends Exception {
        public ReadFailure(String message) {
            super(message);
        }

        public ReadFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
