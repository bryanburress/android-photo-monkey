package com.chesapeaketechnology.photomonkey.model;

/**
 * Encapsulates functionality related to reading and writing the metadata related to an image.
 *
 * @since 0.2.0
 */
public abstract class AMetadataDelegate
{

    public static AMetadataDelegate defaultMetadataDelegate()
    {
        return new ExifMetadataDelegate();
    }

    /**
     * Support for future alternative storage mechanisms (database, sidecar, etc).
     *
     * @param strategy the persistence strategy to be used for the data
     * @return
     */
    public static AMetadataDelegate getDelegate(PersistenceStrategy strategy)
    {
        if (strategy == PersistenceStrategy.EXIF)
        {
            return new ExifMetadataDelegate();
        }
        return defaultMetadataDelegate();
    }

    /**
     * Save the metadata for the image.
     *
     * @param metadata the {@link Metadata} to save for the image
     * @param forImage the {@link Image} for which the metadata should be saved
     * @throws SaveFailure if there is an issue saving the metadata
     */
    abstract void save(Metadata metadata, Image forImage) throws SaveFailure;

    /**
     * Read the data for the image into a Metadata object.
     *
     * @param fromImage the {@link Image} for which we want to read the metadata
     * @return A {@link Metadata} object populated based of the provided image.
     * @throws ReadFailure if there is an error reading the metadata for the image.
     */
    abstract Metadata read(Image fromImage) throws ReadFailure;

    /**
     * Currently, the only supported persistence strategy is EXIF.
     */
    public enum PersistenceStrategy
    {
        EXIF
    }

    /**
     * Indicates that there was an issue saving the image metadata.
     */
    public static class SaveFailure extends Exception
    {
        public SaveFailure(String message)
        {
            super(message);
        }

        public SaveFailure(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Indicates there was an issue reading the image metadata.
     */
    public static class ReadFailure extends Exception
    {
        public ReadFailure(String message)
        {
            super(message);
        }

        public ReadFailure(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
