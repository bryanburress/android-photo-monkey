package com.chesapeaketechnology.photomonkey.util;

import static com.chesapeaketechnology.photomonkey.util.PreferenceUtils.getBaseUrl;

import android.content.Context;

import com.chesapeaketechnology.photomonkey.service.RetryCallAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Wrapper for easier access to Rest photo upload API
 *
 * @since 0.2.0
 */
public class PhotoUploadApiUtils
{
    public static final String PHOTOMONKEY_API_VERSION = "0.1.0";

    private static Retrofit retrofit = null;
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context) -> new JsonPrimitive(new String(src)))
            .registerTypeAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) -> json == null ? null : json.getAsString() == null ? null : json.getAsString().getBytes())
            .create();


    public static synchronized Retrofit getRetrofitInstance(Context context)
    {
        if (retrofit == null)
        {
            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(getBaseUrl(PreferenceUtils.getPostEndpointPreference(context)))
                    .addCallAdapterFactory(RetryCallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(GSON))
                    .build();
        }
        return retrofit;
    }
}
