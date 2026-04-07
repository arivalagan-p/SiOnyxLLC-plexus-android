package com.sionyx.plexus.ui.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sionyx.plexus.ui.api.interfaces.CommunicationInterface;
import com.sionyx.plexus.ui.api.interfaces.ForgotPasswordResultCallback;
import com.sionyx.plexus.ui.api.interfaces.ProductUploadResultCallback;
import com.sionyx.plexus.ui.api.interfaces.ProfileDevicesResultCallback;
import com.sionyx.plexus.ui.api.requestModel.RequestChangeNewPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestForgotPassword;
import com.sionyx.plexus.ui.api.requestModel.RequestProfileDeviceModel;
import com.sionyx.plexus.ui.api.responseModel.ErrorResponse;
import com.sionyx.plexus.ui.api.responseModel.ForgotPasswordResponse;
import com.sionyx.plexus.ui.api.responseModel.GetProfileResponse;
import com.sionyx.plexus.utils.Constants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AWSCommunication {
    public AWSCommunication() {
    }

    private Retrofit getRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(provideOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okhttpClientBuilder = new OkHttpClient.Builder();
        okhttpClientBuilder.addInterceptor(logging);
        okhttpClientBuilder.connectTimeout(30, TimeUnit.MINUTES);
        okhttpClientBuilder.readTimeout(30, TimeUnit.SECONDS);
        okhttpClientBuilder.writeTimeout(30, TimeUnit.SECONDS);
        return okhttpClientBuilder.build();
    }

    public void sentForgotPasswordOtp(RequestForgotPassword requestForgotPassword, final ForgotPasswordResultCallback callback) {
        Retrofit retrofit = getRetrofit(Constants.FORGOT_PASSWORD_URL);
        CommunicationInterface api = retrofit.create(CommunicationInterface.class);
        Call<ForgotPasswordResponse> forgotPasswordResponseCall = api.sentForgotPasswordOtp(requestForgotPassword);
        forgotPasswordResponseCall.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPasswordResponse> call, @NonNull Response<ForgotPasswordResponse> response) {
                if (response.isSuccessful()) {
                    try {
                        // Parse the success response body
                        int statusCode = response.code();
                        ForgotPasswordResponse myResponse = response.body();
                        String message = myResponse.getMessage();
                        callback.onSuccess(message, statusCode);
                        Log.d("TAG", "statusCode: "+ statusCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Parse the error response body
                        int statusCode = response.code();
                        Gson gson = new Gson();
                        ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        String errorMessage = errorResponse.getError();
                        String getMessage = errorResponse.getMessage();
                        callback.onSuccess(getMessage, statusCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForgotPasswordResponse> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void changeNewPassword(RequestChangeNewPassword requestChangeNewPassword, final ForgotPasswordResultCallback callback) {
        Retrofit retrofit = getRetrofit(Constants.CHANGE_NEW_PASSWORD_URL);
        CommunicationInterface api = retrofit.create(CommunicationInterface.class);
        Call<ForgotPasswordResponse> confirmForgotPasswordResponseCall = api.changeNewPassword(requestChangeNewPassword);
        confirmForgotPasswordResponseCall.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPasswordResponse> call, @NonNull Response<ForgotPasswordResponse> response) {
                if (response.isSuccessful()) {
                    try {
                        // Parse the success response body
                        ForgotPasswordResponse myResponse = response.body();
                        String message = myResponse.getMessage();
                        callback.onSuccess(message, 200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Parse the error response body
                        Gson gson = new Gson();
                        ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        String errorMessage = errorResponse.getError();
                        String getMessage = errorResponse.getMessage();
                        Log.d("TAG", "onResponse error getMessage: " + getMessage);
                        Log.d("TAG", "onResponse errorResponse: " + errorMessage);
                        callback.onFailure(getMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForgotPasswordResponse> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void getListOfProductDevices(String userName, final ProfileDevicesResultCallback callback) {
        Retrofit retrofit = getRetrofit(Constants.PROFILE_URL);
        CommunicationInterface api = retrofit.create(CommunicationInterface.class);
        Call<GetProfileResponse> getProfileResponseCall = api.getListOfProductDevice(userName);
        getProfileResponseCall.enqueue(new Callback<GetProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<GetProfileResponse> call, @NonNull Response<GetProfileResponse> response) {
                if (response.isSuccessful()) {
                    try {
                        // Parse the success response body
                        GetProfileResponse profileResponse = response.body();
                        callback.onSuccess(profileResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Parse the error response body
                        Gson gson = new Gson();
                        ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        String errorMessage = errorResponse.getError();
                        String message = errorResponse.getMessage();
                        callback.onFailure(errorMessage, message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetProfileResponse> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage(), "");
            }
        });
    }

    public void uploadProductToAws(RequestProfileDeviceModel profileDeviceModel, final ProductUploadResultCallback callback) {
        Retrofit retrofit = getRetrofit(Constants.PROFILE_URL);
        CommunicationInterface api = retrofit.create(CommunicationInterface.class);
        Call<ForgotPasswordResponse> getProfileResponseCall = api.uploadProductToAws(profileDeviceModel);
        getProfileResponseCall.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPasswordResponse> call, @NonNull Response<ForgotPasswordResponse> response) {
                if (response.isSuccessful()) {
                    try {
                        // Parse the success response body
                        ForgotPasswordResponse myResponse = response.body();
                        String message = myResponse.getMessage();
                        callback.onSuccess(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Parse the error response body
                        Gson gson = new Gson();
                        ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        String errorMessage = errorResponse.getError();
                        String message = errorResponse.getMessage();
                        callback.onFailure(errorMessage, message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForgotPasswordResponse> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage(), "");
            }
        });
    }

    public void deleteProductOnAws(RequestProfileDeviceModel requestProfileDeviceModel, final ProductUploadResultCallback callback) {
        Retrofit retrofit = getRetrofit(Constants.PROFILE_URL);
        CommunicationInterface api = retrofit.create(CommunicationInterface.class);
        Call<ForgotPasswordResponse> getProfileResponseCall = api.deleteProductOnAws(requestProfileDeviceModel);
        getProfileResponseCall.enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPasswordResponse> call, @NonNull Response<ForgotPasswordResponse> response) {
                if (response.isSuccessful()) {
                    try {
                        // Parse the success response body
                        ForgotPasswordResponse myResponse = response.body();
                        String message = myResponse.getMessage();
                        callback.onSuccess(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // Parse the error response body
                        Gson gson = new Gson();
                        ErrorResponse errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                        String errorMessage = errorResponse.getError();
                        String message = errorResponse.getMessage();
                        callback.onFailure(errorMessage, message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForgotPasswordResponse> call, @NonNull Throwable t) {
                callback.onFailure(t.getMessage(), "");
            }
        });
    }
}
