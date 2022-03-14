package com.chesapeaketechnology.photomonkey.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * The interface for posting photos to the Azure Rest endpoint
 *
 * @since 0.2.0
 */
public interface PhotoUploadService
{
    @Retry(max = 4)
    @POST("api/UploadFile")
    Call<ResponseBody> postPhoto(@Body Photo photo);
}
