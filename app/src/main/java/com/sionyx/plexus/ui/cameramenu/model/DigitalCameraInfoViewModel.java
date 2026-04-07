package com.sionyx.plexus.ui.cameramenu.model;

import static com.sionyx.plexus.utils.Constants.apiErrorMessage;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.repository.DigitalCameraInfoRepository;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.Event;
import com.sionyx.plexus.ui.api.digitalcamerainformation.RestApiClient;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResetCallback;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.CameraResponseCallback;
import com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback.PasswordResponseCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.util.Objects;

import io.reactivex.Single;

public class DigitalCameraInfoViewModel extends AndroidViewModel {
    private final DigitalCameraInfoRepository digitalCameraInfoRepository;
    private final String TAG = "DigitalCameraInfoViewModel";
    private final Application application;
    private String cameraPassword = "";
    private final String BASE_VERSION = "2.1.6";

    private final MutableLiveData<com.sionyx.plexus.utils.Event<String>> _cameraResponse = new MutableLiveData<>();
    public LiveData<com.sionyx.plexus.utils.Event<String>> cameraResponse = _cameraResponse;

    public DigitalCameraInfoViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        this.digitalCameraInfoRepository = new DigitalCameraInfoRepository(application);
    }
    public final MutableLiveData<Event<Object>> cameraDetails = new MutableLiveData<>();

    public LiveData<Event<Object>> _cameraDetails = cameraDetails;


    private RestApiClient restApiClient = null;

    private RestApiClient getRestApiClient() {
        if (restApiClient == null) {
            restApiClient = new RestApiClient();
        }
        return restApiClient;
    }

    public void getDigitalCameraInformation(CameraResponseCallback cameraResponseCallback) {
        getRestApiClient().getCameraResponse(new CameraResponseCallback() {
            @Override
            public void onSuccess(String responseStr, int code) {
                Log.e(TAG, "onResponse code " + code);
                cameraResponseCallback.onSuccess(responseStr,code);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "onFailure message " + message);
                cameraResponseCallback.onFailure(message);
            }
        });
    }

    // use jsoup method if xmlResponse is in xml format not json format
    public JSONObject convertXmlToJson(String xmlResponse) {
        JSONObject json = new JSONObject();
        try {
            Document doc = Jsoup.parse(xmlResponse, "", Parser.xmlParser());
            Element root = doc.selectFirst("device");
            if (root != null) {
                json.put("modelName", Objects.requireNonNull(root.selectFirst("modelName")).text());
                json.put("modelNumber", Objects.requireNonNull(root.selectFirst("modelNumber")).text());
                json.put("serialNumber", Objects.requireNonNull(root.selectFirst("serialNumber")).text());
                json.put("manufacturer", Objects.requireNonNull(root.selectFirst("manufacturer")).text());
            }
            Log.d(TAG, "convertXmlToJson : " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public void getCameraServerPassword(String ssid, PasswordResponseCallback callback){
        //api/wireless?wifi.PWD , "api/wireless?group=wifi&cmd=get" , api/wireless?cmd=get  , api/wireless?service=wireless&group=wifi&cmd=get
        getRestApiClient().getCameraPassword(ssid, new PasswordResponseCallback() {
            @Override
            public void onSuccess(String responseStr, int responseCode) {
                Log.e(TAG, "getCameraPassword onResponse code : " + responseCode +" response : " + responseStr);
                if (responseCode == Constants.ON_SUCCESS) {
                    JSONObject obj = null;
                    try {
                        obj = new JSONObject(responseStr);

                        if (obj.getString("type").equals("done") && obj.getJSONObject("reply").getString("errno").equals("0")) {
                            JSONObject value = obj.getJSONObject("reply");
                            String password = value.optString("value");
                            callback.onSuccess(password,responseCode);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "exception " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "getCameraPassword onFailure " + message);
                callback.onFailure(message);
            }
        });
    }

    public void postCameraPassword(String password, PasswordResponseCallback callback){
        getRestApiClient().postCameraPassword(password, new PasswordResponseCallback() {
            @Override
            public void onSuccess(String responseMessage, int responseCode) {
                Log.e(TAG, "setCameraPassword onResponse " + responseCode);
                callback.onSuccess(responseMessage,responseCode);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "setCameraPassword onFailure " + message);
                callback.onFailure(message);
            }
        });
    }

    public void setCameraFactoryReset(CameraResetCallback callback){
        getRestApiClient().setCameraFactoryReset(new CameraResetCallback() {
            @Override
            public void onSuccess(boolean isReset, int responseCode) {
                Log.e(TAG, "setCameraFactoryReset onResponse " + responseCode);
                callback.onSuccess(isReset,responseCode);
            }

            @Override
            public void onFailure(String onFailure) {
                Log.e(TAG, "setCameraFactoryReset onFailure " + onFailure);
                callback.onFailure(onFailure);
            }
        });
    }

    public void setCameraFactoryReboot(CameraResetCallback callback){
        getRestApiClient().setCameraFactoryReboot(new CameraResetCallback() {
            @Override
            public void onSuccess(boolean isReset, int responseCode) {
                Log.e(TAG, "setCameraFactoryReboot onResponse " + responseCode);
                callback.onSuccess(isReset,responseCode);
            }

            @Override
            public void onFailure(String onFailure) {
                Log.e(TAG, "setCameraFactoryReboot onFailure " + onFailure);
                callback.onFailure(onFailure);
            }
        });
    }


    public void updateCameraConnectAndPassword(String ssid, String pwd, int isAutoConnectState) {
        digitalCameraInfoRepository.updateCameraAutoConnectStateAndPassword(ssid, pwd, isAutoConnectState);
    }

    public void updateCameraAutoConnect(String ssid, int isAutoConnectState) {
        digitalCameraInfoRepository.updateCameraAutoConnect(ssid, isAutoConnectState);
    }

    public void updateCameraPassword(String ssid, String pwd) {
        Log.i(TAG,"updateCameraPassword ssid " + ssid + " password" + pwd);
        digitalCameraInfoRepository.updateCameraPassword(ssid, pwd);
    }

    public Single<String> getCameraPassword(String ssid){
        return digitalCameraInfoRepository.getCameraPassword(ssid);
    }

    public void insertNWDCamera(NightwaveDigitalWiFiHistory profile) {
        digitalCameraInfoRepository.insertNWDCamera(profile);
    }
    public Single<NightwaveDigitalWiFiHistory> checkNWDSsidIsExit(String ssid) {
        return digitalCameraInfoRepository.checkNWDSsidIsExit(ssid);
    }
    public void updateLastFWDisplayDate(String ssid, long lastDisplayDate) {
        digitalCameraInfoRepository.updateLastFWDisplayDate(ssid, lastDisplayDate);
    }

    public void updateCameraFirmwareVersion(String ssid, String firmware) {
        digitalCameraInfoRepository.updateCameraFirmwareVersion(ssid, firmware);
    }
    public void deleteNWDSsid(String ssid) {
        digitalCameraInfoRepository.deleteNWDSsid(ssid);
    }


    public void getUpdatedPasswordFromCamera(String selectedSsid, Context context){
        getCameraServerPassword(selectedSsid, new PasswordResponseCallback() {

            @Override
            public void onSuccess(String password, int responseCode) {
                Log.e(TAG,"getCameraServerPassword onSuccess " + responseCode + " password : " + password);
                if (responseCode == Constants.ON_SUCCESS){
                    cameraPassword = password;
                    updateCameraPassword(selectedSsid, password);
                    _cameraResponse.postValue(new com.sionyx.plexus.utils.Event<>(cameraPassword));
                } else {
                    apiErrorMessage(context,responseCode);
                }
            }
            @Override
            public void onFailure(String message) {
                Log.e(TAG,"getCameraServerPassword onFailure " + message);
                apiErrorMessage(context,0);
            }
        });

    }


    // To open the wifi setting screen for change password the current camera firmware version should be greater than BASE_VERSION for NWD.
    public enum CompareResult {
        GREATER, // base > newVersion
        EQUAL,   // base == newVersion
        LESSER   // base < newVersion
    }

    public  CompareResult compareWithBase(String newVersion) {
        String baseClean = cleanVersion(BASE_VERSION);
        String newClean = cleanVersion(newVersion);

        int result = compareVersionNumbers(newClean, baseClean);

        if (result < 0) {
            return CompareResult.LESSER;   // newVersion < base
        } else if (result > 0) {
            return CompareResult.GREATER;  // newVersion > base
        } else {
            return CompareResult.EQUAL;    // same version
        }
    }

    private String cleanVersion(String version) {
        if (version.contains("-")) {
            version = version.substring(0, version.indexOf("-"));
        }
        return version;
    }

    private int compareVersionNumbers(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;

            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0;
    }

    public boolean isBuildVersionHigher(String current, String target) {
        String numericCurrent = stripSuffix(current);
        String numericTarget = stripSuffix(target);

        Log.i(TAG, "Comparing build versions: current=" + current + ", target=" + target);

        String[] curParts = numericCurrent.split("\\.");
        String[] tarParts = numericTarget.split("\\.");

        int len = Math.max(curParts.length, tarParts.length);
        for (int i = 0; i < len; i++) {
            int curVal = i < curParts.length ? parseIntSafe(curParts[i]) : 0;
            int tarVal = i < tarParts.length ? parseIntSafe(tarParts[i]) : 0;

            if (curVal < tarVal) {
                return true;
            }
            if (curVal > tarVal) {
                return false;
            }
        }
        return false;
    }


    // Strips suffix like "-beta6" or "-alpha" from last part
    private String stripSuffix(String version) {
        String[] parts = version.split("\\.");
        StringBuilder cleanVersion = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String numeric = part.replaceAll("[^0-9]", ""); // remove suffix
            cleanVersion.append(numeric.isEmpty() ? "0" : numeric);
            if (i < parts.length - 1) cleanVersion.append(".");
        }

        return cleanVersion.toString();
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
