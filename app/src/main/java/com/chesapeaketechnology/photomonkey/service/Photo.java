package com.chesapeaketechnology.photomonkey.service;

import com.chesapeaketechnology.photomonkey.util.PhotoUploadApiUtils;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Represents a photo to be uploaded to a Rest API endpoint
 * @since 0.2.0
 */
public class Photo implements Serializable
{
    @SerializedName("version")
    private String version;

    @SerializedName("filename")
    private String filename;

    @SerializedName("content")
    private String content;

    @SerializedName("device_id")
    private String deviceID;

    public Photo()
    {
    }

    public Photo(String filename, String content, String deviceID)
    {
        version = PhotoUploadApiUtils.PHOTOMONKEY_API_VERSION;
        this.filename = filename;
        this.content = content;
        this.deviceID  = deviceID;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getDeviceID()
    {
        return deviceID;
    }

    public void setDeviceID(String deviceID)
    {
        this.deviceID = deviceID;
    }
}
