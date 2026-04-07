package com.sionyx.plexus.ui.api.digitalcamerainformation;

import static com.dome.librarynightwave.utils.Constants.BASE_URL_CAMERA_VALID_ACTION;
import static com.dome.librarynightwave.utils.Constants.URL_CAMERA_RESET_ACTION;
import static com.dome.librarynightwave.utils.Constants.URL_CAMERA_VALID_ACTION_JSON_RETURNS;
import static com.dome.librarynightwave.utils.Constants.URL_GET_PASSWORD_ACTION;
import static com.dome.librarynightwave.utils.Constants.URL_SET_PASSWORD_ACTION;
import static com.dome.librarynightwave.utils.Constants.setUpdateTransferProtocol;

import android.util.Log;

import androidx.annotation.NonNull;

import com.dome.librarynightwave.utils.Constants;
import com.sionyx.plexus.ApplicationClass;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResetCallback;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResponseCallback;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.PasswordResponseCallback;
import com.sionyx.plexus.utils.CameraHostnameVerifier;
import com.sionyx.plexus.utils.CameraSSLSocketFactory;
import com.sionyx.plexus.utils.TrustManagerUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class RestApiClient {

    private static Retrofit retrofit = null;

    private final String TAG = "RestApiClient";

    private final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static Retrofit getInstance(String baseUrl) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(provideOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;

    }

    private static OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder okhttpClientBuilder = new OkHttpClient.Builder();
        okhttpClientBuilder.addInterceptor(logging);
        okhttpClientBuilder.connectTimeout(5, TimeUnit.SECONDS);
        okhttpClientBuilder.readTimeout(5, TimeUnit.SECONDS);
        okhttpClientBuilder.writeTimeout(5, TimeUnit.SECONDS);

        try {
            // Load SSL factory using our helper
            SSLSocketFactory sslSocketFactory = CameraSSLSocketFactory.getSSLSocketFactory(ApplicationClass.appContext.getApplicationContext());

            if (sslSocketFactory != null) {
                X509TrustManager trustManager = new TrustManagerUtils()
                        .getSystemDefaultTrustManager();
                okhttpClientBuilder.sslSocketFactory(sslSocketFactory, trustManager);
            }
            okhttpClientBuilder.hostnameVerifier(new CameraHostnameVerifier());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return okhttpClientBuilder.build();
    }

    //"xml/description.xml" xml response returns
   /* public void getCameraResponse(CameraResponseCallback callback) {
        Retrofit retrofit = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit.create(CameraApi.class);
        String cameraInfoUrl = Constants.getLoadUrl(URL_CAMERA_VALID_ACTION);
        api.getCameraInfoXml(cameraInfoUrl, "", "xml/description.xml").enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseStr = null;
                    if (response.body() != null) {
                        responseStr = response.body().string();
                    }
                    callback.onSuccess(responseStr,response.code());

                } catch ( Exception e){
                    Log.e(TAG,"exception " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                callback.onFailure(throwable.getMessage());
            }
        });
    }*/

    ////"api/operation?system.information" json response returns
    public void getCameraResponse(CameraResponseCallback callback) {
        Retrofit retrofit = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit.create(CameraApi.class);
        String cameraInfoUrl = Constants.getLoadUrl(URL_CAMERA_VALID_ACTION_JSON_RETURNS); // temporary use
        api.getCameraInfo(cameraInfoUrl, "", CONTENT_TYPE).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                String responseStr = null;
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        responseStr = responseBody.string();
                    }
                    Log.e(TAG, "getCameraResponse onResponse  " + responseStr);
                    callback.onSuccess(responseStr, response.code());
                } catch (IOException e) {
                    Log.e(TAG, "getCameraResponse exception " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "getCameraResponse onFailure  " + throwable.getMessage());
                setUpdateTransferProtocol(Constants.UpdateTransferProtocol.http); // Fallback to http request when https failed
                // Error : HTTP FAILED: java.net.ConnectException: Failed to connect to /172.30.1.1:443.
                fallbackToHttp();
            }

            private void fallbackToHttp() {
                Retrofit retrofit = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
                CameraApi api = retrofit.create(CameraApi.class);
                String cameraInfoUrl = Constants.getLoadUrl(URL_CAMERA_VALID_ACTION_JSON_RETURNS); // temporary use
                api.getCameraInfo(cameraInfoUrl, "", CONTENT_TYPE).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        String responseStr = null;
                        try (ResponseBody responseBody = response.body()) {
                            if (responseBody != null) {
                                responseStr = responseBody.string();
                            }
                            Log.e(TAG, "getCameraResponse onResponse  " + responseStr);
                            callback.onSuccess(responseStr, response.code());
                        } catch (IOException e) {
                            Log.e(TAG, "getCameraResponse exception " + e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                        Log.e(TAG, "getCameraResponse onFailure  " + throwable.getMessage());
                        callback.onFailure(throwable.getMessage());
                    }
                });
            }
        });
    }

    public void getCameraPassword(String cameraSsid, PasswordResponseCallback callback) {
        Retrofit retrofit1 = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit1.create(CameraApi.class);
        api.getCameraPassword(Constants.getLoadUrl(URL_GET_PASSWORD_ACTION), "", CONTENT_TYPE).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                String responseStr = null;
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        responseStr = responseBody.string();
                    }
                    Log.e(TAG, "getCameraPassword onResponse  " + responseStr);
                    callback.onSuccess(responseStr, response.code());
                } catch (IOException e) {
                    Log.e(TAG, "exception " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "getCameraPassword onFailure  " + throwable.getMessage());
                callback.onFailure(throwable.getMessage());
            }
        });
    }

    public void postCameraPassword(String postPwd, PasswordResponseCallback callback) {
        Retrofit retrofit1 = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit1.create(CameraApi.class);
        RequestBody requestBody = RequestBody.create(
                "wifi.pwd=" + postPwd,
                MediaType.parse(CONTENT_TYPE)
        );

        api.setCameraPassword(Constants.getLoadUrl(URL_SET_PASSWORD_ACTION), "",CONTENT_TYPE, requestBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.e(TAG, "postCameraPassword onResponse  " + response.message());
                callback.onSuccess(response.message(), response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "postCameraPassword onFailure  " + throwable.getMessage());
                callback.onFailure(throwable.getMessage());
            }
        });
    }

    public void setCameraFactoryReset(CameraResetCallback callback) {
        Retrofit retrofit1 = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit1.create(CameraApi.class);

        RequestBody requestBody = RequestBody.create(
                "system.factoryRestore",
                MediaType.parse(CONTENT_TYPE)
        );
        api.setCameraFactoryReset(Constants.getLoadUrl(URL_CAMERA_RESET_ACTION), "", CONTENT_TYPE, requestBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.e(TAG, "setCameraFactoryReset onResponse  " + response.code() + " message " + response.message());
                callback.onSuccess(true, response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "setCameraFactoryReset onFailure  " + throwable.getMessage());
                callback.onFailure(throwable.getMessage());
            }
        });

    }

    public void setCameraFactoryReboot(CameraResetCallback callback) {
        Retrofit retrofit = RestApiClient.getInstance(Constants.getLoadUrl(BASE_URL_CAMERA_VALID_ACTION));
        CameraApi api = retrofit.create(CameraApi.class);

        RequestBody requestBody = RequestBody.create(
                "system.reboot",
                MediaType.parse(CONTENT_TYPE)
        );
        api.setCameraFactoryReboot(Constants.getLoadUrl(URL_CAMERA_RESET_ACTION), "", CONTENT_TYPE, requestBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.e(TAG, "setCameraFactoryReboot onResponse  " + response.code() + " message " + response.message());
                callback.onSuccess(true, response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.e(TAG, "setCameraFactoryReboot onFailure  " + throwable.getMessage());
                callback.onFailure(throwable.getMessage());
            }
        });

    }

}
