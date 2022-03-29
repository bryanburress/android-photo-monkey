package com.chesapeaketechnology.photomonkey.service;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * The interface for posting photos to the Azure Rest endpoint
 *
 * @since 0.2.0
 */
public interface PhotoUploadService
{
    @Retry(max = 4)
    @POST("{optionalPathArgs}")
    Call<ResponseBody> postPhoto(
            @Body Photo photo,
            @Path(value = "optionalPathArgs", encoded = true) String optionalPathArgs,
            @QueryMap Map<String, String> optionalQueryArgs
    );
}
