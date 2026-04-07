package com.sionyx.plexus.ui.api.digitalcamerainformation;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface CameraApi {

    @GET
    Call<ResponseBody> getCameraInfo(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType // Set Content-Type header
    );

    @GET
    Call<ResponseBody> getCameraInfoXml(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType // Set Content-Type header
    );

    @GET
    Call<ResponseBody> getCameraPassword(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType // Set Content-Type header
    );

    @POST
    Call<ResponseBody> setCameraPassword(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType, // Set Content-Type header
            @Body RequestBody body
    );

    @POST
    Call<ResponseBody> setCameraFactoryReset(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType, // Set Content-Type header
            @Body RequestBody body
    );

    @POST
    Call<ResponseBody> setCameraFactoryReboot(
            @Url String url,
            @Header("SessionID") String sessionId,
            @Header("Content-Type") String contentType, // Set Content-Type header
            @Body RequestBody body
    );
}

