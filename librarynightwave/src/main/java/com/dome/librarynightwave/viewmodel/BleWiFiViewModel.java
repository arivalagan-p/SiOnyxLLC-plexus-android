package com.dome.librarynightwave.viewmodel;

import static com.dome.librarynightwave.utils.Constants.DEFAULT_CAMERA_PWD;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;

import android.app.Application;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor;
import com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback;
import com.dome.librarynightwave.model.repository.BleRepository;
import com.dome.librarynightwave.model.repository.CameraInfoRepository;
import com.dome.librarynightwave.model.repository.WiFiRepository;
import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.utils.Event;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class BleWiFiViewModel extends AndroidViewModel {
    String TAG = "BleViewModel";
    private final CameraInfoRepository repository;
    private final BleRepository bleRepository;
    private final WiFiRepository wiFiRepository;


    private int isAutoConnectEnabled = -1;

    private boolean isAutoConnectionEnabled = false;
    private String connectedSsidFromWiFiManager = "";
    private String onAvailableSSID = "";
    private final Application application;
    private final Flowable<List<WiFiHistory>> cameraInfoList;
    private final MutableLiveData<Boolean> shouldStartBleDiscovery = new MutableLiveData<>();
    private boolean isAppCleared = false;
    private final MutableLiveData<Boolean> shouldStartToGetWifiHistory = new MutableLiveData<>();
    private final ArrayList<WiFiHistory> combinedCameraList = new ArrayList<>();
    private final MutableLiveData<Event<String>> ssidToRequestWiFiConnection = new MutableLiveData<>();

    public BleWiFiViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        repository = CameraInfoRepository.getInstance(application);
        bleRepository = BleRepository.getInstance(application);
        wiFiRepository = WiFiRepository.getInstance(application);
        cameraInfoList = repository.getCameraList();

    }

    public boolean isAppCleared() {
        return isAppCleared;
    }

    public void setAppClearedFalse() {
        isAppCleared = false;
    }

    public MutableLiveData<Boolean> getShouldStartToGetWifiHistory() {
        return shouldStartToGetWifiHistory;
    }

    public void setShouldStartToGetWifiHistory(boolean value) {
        shouldStartToGetWifiHistory.setValue(value);
    }

    public MutableLiveData<Boolean> getShouldStartBleDiscovery() {
        return shouldStartBleDiscovery;
    }

    public void setShouldStartBleDiscovery(boolean value) {
        shouldStartBleDiscovery.setValue(value);
    }

    public MutableLiveData<Event<String>> getSsidToRequestWiFiConnection() {
        return ssidToRequestWiFiConnection;
    }

    public void setSsidToRequestWiFiConnection(String ssid) {
        ssidToRequestWiFiConnection.setValue(new Event<>(ssid));
    }

    public ArrayList<WiFiHistory> getCombinedCameraList() {
        return combinedCameraList;
    }


    public String getConnectedSsidFromWiFiManager() {
        return connectedSsidFromWiFiManager;
    }

    public void setConnectedSsidFromWiFiManager(String connectedSsidFromWiFiManager) {
        this.connectedSsidFromWiFiManager = connectedSsidFromWiFiManager;

    }

    public String getOnAvailableSSID() {
        return onAvailableSSID;
    }

    public void setOnAvailableSSID(String onAvailableSSID) {
        this.onAvailableSSID = onAvailableSSID;
    }

    public int getIsAutoConnectEnabled() {
        return isAutoConnectEnabled;
    }

    public void setIsAutoConnectEnabled(int isAutoConnectEnabled) {
        this.isAutoConnectEnabled = isAutoConnectEnabled;
    }

    public boolean isAutoConnectionEnabled() {
        return isAutoConnectionEnabled;
    }

    public void setShouldAutoConnectionEnabled(boolean shouldConnectWithSocket) {
        this.isAutoConnectionEnabled = shouldConnectWithSocket;
    }


    /*Camera History*/
    public void insertCamera(WiFiHistory profile) {
        repository.insertCamera(profile);
    }

    public void updateCameraLastConnectedTimeAndState(String ssid, long timeStamp, int state) {
        repository.updateCameraLastConnectedTimeAndState(ssid, timeStamp, state);
    }

    public void updateLastConnectedCameraState(String ssid) {
        repository.updateLastConnectedCameraState(ssid);
    }

    public void updateCameraLastConnectedTime(String ssid, long timeStamp) {
        repository.updateCameraLastConnectedTime(ssid, timeStamp);
    }

    public void updateCameraConnectionState(String ssid, int state) {
        repository.updateCameraConnectionState(ssid, state);
    }

    public void updateAllCameraConnectionState(int state) {
        repository.updateAllCameraConnectionState(state);
    }

    public void deleteSsid(String ssid) {
        repository.deleteSsid(ssid);
    }

    public Flowable<List<WiFiHistory>> getCameraList() {
        return cameraInfoList;
    }

    public Single<String> getCameraName(String ssid) {
        return repository.getCameraName(ssid);
    }

    public Single<List<WiFiHistory>> getAll() {
        return repository.getAll();
    }

    public Single<WiFiHistory> checkSsidIsExit(String ssid) {
        return repository.checkSsidIsExit(ssid);
    }

    public Maybe<WiFiHistory> checkSsidDiscoverable(String ssid) {
        return repository.checkSsidDiscoverable(ssid);
    }

    public boolean hasAlreadyExistSSId(String ssid) {
        return repository.hasAlreadyExistSSId(ssid);
    }

    public void updateCameraName(String ssid, String cameraName) {
        repository.updateCameraName(ssid, cameraName);
    }
    /*Bluetooth*/
    public LiveData<Boolean> isBluetoothSupported() {
        return bleRepository.isBluetoothSupported();
    }

    public LiveData<Boolean> isBluetoothDisabled() {
        return bleRepository.isBluetoothDisabled();
    }

    public LiveData<Boolean> startObserveBleOnOffState() {
        return bleRepository.startObserveBleOnOffState();
    }

    public MutableLiveData<Event<Boolean>> getIsBleScanning() {
        return bleRepository.getIsBleScanning();
    }

    public boolean isScanning() {
        return bleRepository.isScanning();
    }

    public void stopObserveBleOnOffState() {
        bleRepository.stopObserveBleOnOffState();
    }

    public void startBleDiscovery() {
        bleRepository.startBleDiscovery();
    }
    public void stopBleDiscovery() {
        bleRepository.stopBleDiscovery();
    }
    public LiveData<Object> getBleScanResult() {
        return bleRepository.getBleScanResult();
    }

    public void getPairedDevices() {
        bleRepository.getPairedDevices();
    }

    /*WiFi*/
    public LiveData<Boolean> isWiFiEnabled() {
        return wiFiRepository.isWiFiEnabled();
    }

    public LiveData<Boolean> startObserveWiFiOnOffState() {
        return wiFiRepository.startObserveWiFiOnOffState();
    }

    public void stopObserveWiFiOnOffState() {
        wiFiRepository.stopObserveWiFiOnOffState();
    }

    public WiFiConnectionStateMonitor observeConnectionStateMonitor() {
        return wiFiRepository.observeConnectionStateMonitor();
    }

    public void unRegisterConnectionStateMonitor(){
        wiFiRepository.unRegisterConnectionStateMonitor();
    }
    public void removeConnectionStateMonitor() {
        wiFiRepository.removeConnectionStateMonitor();
    }
    public void resetWifiState() {
       wiFiRepository.resetWifiState();
    }

    public MutableLiveData<WiFiNetworkCallback> getNetworkCallbackMutableLiveData() {
        return wiFiRepository.getNetworkCallbackMutableLiveData();
    }

    public Object getNetworkCapability() {
        return wiFiRepository.getNetworkCapability();
    }

    public void setNetworkCapability() {
        wiFiRepository.setNetworkCapability();
    }

    public NetworkRequest getNetworkRequest(String ssid, String nwdCameraPassword) {
        if(ssid == null || ssid.isEmpty())
            return null;

        WifiManager mWifiManager = (WifiManager) application.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean staApConcurrencySupported = false,
                staConcurrencyForLocalOnlyConnectionsSupported = false,
                staBridgedApConcurrencySupported = false,
                staConcurrencyForMultiInternetSupported = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            staApConcurrencySupported = mWifiManager.isStaApConcurrencySupported();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            staConcurrencyForLocalOnlyConnectionsSupported = mWifiManager.isStaConcurrencyForLocalOnlyConnectionsSupported();
            staBridgedApConcurrencySupported = mWifiManager.isStaBridgedApConcurrencySupported();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            staConcurrencyForMultiInternetSupported = mWifiManager.isStaConcurrencyForMultiInternetSupported();
        }
        String currentCameraPwd = ssid.contains(FILTER_STRING6) ? nwdCameraPassword : DEFAULT_CAMERA_PWD; // change pwd
        Log.e(TAG, "getNetworkRequest: SSID: " + ssid
                + " StaApConcurrencySupported: " + staApConcurrencySupported
                + " StaConcurrencyForLocalOnlyConnectionsSupported: " + staConcurrencyForLocalOnlyConnectionsSupported
                + " StaBridgedApConcurrencySupported: " + staBridgedApConcurrencySupported
                + " StaConcurrencyForMultiInternetSupported: " + staConcurrencyForMultiInternetSupported
                + " currentCameraPassword: " + currentCameraPwd
        );

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(currentCameraPwd)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();
        }else {
            return new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();
        }

    }

    public NetworkRequest getNetworkRequestToMonitor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            return new NetworkRequest.Builder()
//                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
//                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
//                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                    .build();

            return new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
        } else {
            return new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
        }
    }

    @Override
    protected void onCleared() {
        setOnAvailableSSID("");
        setConnectedSsidFromWiFiManager("");
        setIsAutoConnectEnabled(-1);
        isAppCleared = true;
        repository.updateCameraConnectionState(-1);

        Log.e(TAG, "onCleared: Called");
        super.onCleared();
    }
}