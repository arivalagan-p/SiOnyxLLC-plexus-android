package com.sionyx.plexus.ui.api.interfaces;

import com.sionyx.plexus.ui.api.requestModel.RequestChangeNewPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestForgotPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestProfileDeviceModel;
import com.sionyx.plexus.ui.api.responseModel.ForgotPasswordResponse;
import com.sionyx.plexus.ui.api.responseModel.GetProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CommunicationInterface {
    @Headers("Content-Type:application/json")
    @POST("ResetPassword")
    Call<ForgotPasswordResponse> sentForgotPasswordOtp(@Body RequestForgotPassword requestForgotPassword);

    @Headers("Content-Type:application/json")
    @POST("Setpassword")
    Call<ForgotPasswordResponse> changeNewPassword(@Body RequestChangeNewPassword requestChangeNewPassword);

    @Headers("Content-Type:application/json")
    @GET("QR-ScannerDetails")
    Call<GetProfileResponse> getListOfProductDevice(@Query("username") String username);

    @Headers("Content-Type:application/json")
    @POST("QR-ScannerDetails")
    Call<ForgotPasswordResponse> uploadProductToAws(@Body RequestProfileDeviceModel profileDeviceModel);

    @Headers("Content-Type:application/json")
    @HTTP(method = "DELETE", path = "QR-ScannerDetails", hasBody = true)
    Call<ForgotPasswordResponse> deleteProductOnAws(@Body RequestProfileDeviceModel requestProfileDeviceModel);
}

