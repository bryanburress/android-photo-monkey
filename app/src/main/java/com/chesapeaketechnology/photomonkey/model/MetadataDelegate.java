package com.chesapeaketechnology.photomonkey.model;

public abstract class MetadataDelegate {
    public enum PersistenceStrategy {
        EXIF
    }

    public static MetadataDelegate defaultMetadataDelegate() {
        return new ExifMetadataDelegate();
    }

    public static MetadataDelegate getDelegate(PersistenceStrategy strategy) {
        if(strategy == PersistenceStrategy.EXIF) {
            return new ExifMetadataDelegate();
        }
        return defaultMetadataDelegate();
    }

    public MetadataDelegate() {
    }

    abstract void save(Metadata metadata, Image forImage) throws SaveFailure;
    abstract Metadata read(Image fromImage) throws ReadFailure;


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
