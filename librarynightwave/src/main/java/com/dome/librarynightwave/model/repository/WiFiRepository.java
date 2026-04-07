package com.dome.librarynightwave.model.repository;

import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.CAMERA_NOT_AVAILABLE;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.isCameraAvailableState;
import static com.dome.librarynightwave.utils.Constants.isSDK14;
import static com.dome.librarynightwave.utils.Constants.isSDK15;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;

import android.app.Application;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor;
import com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback;
import com.dome.librarynightwave.model.receivers.wifi.WifiBroadCastReceiver;

public class WiFiRepository {
    String TAG = "WiFiRepository";
    private final Application application;
    private final WifiBroadCastReceiver wifiBroadCastReceiver;
    private WiFiConnectionStateMonitor connectionStateMonitor;
    private static WiFiRepository wiFiRepository;

    public static WiFiRepository getInstance(Application application) {
        if (wiFiRepository == null) {
            wiFiRepository = new WiFiRepository(application);
        }
        return wiFiRepository;
    }

    public WiFiRepository(Application application) {
        this.application = application;
        wifiBroadCastReceiver = new WifiBroadCastReceiver(application);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        if (isSDK14() ||  isSDK15() || isSDK16AndAbove()) {
            ContextCompat.registerReceiver(application, wifiBroadCastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        } else {
            application.registerReceiver(wifiBroadCastReceiver, intentFilter);
        }
    }

    public LiveData<Boolean> isWiFiEnabled() {
        return wifiBroadCastReceiver.isWiFiEnabled();
    }

    public LiveData<Boolean> startObserveWiFiOnOffState() {
        return wifiBroadCastReceiver.isWiFiOnOffLiveState();
    }

    public WiFiConnectionStateMonitor observeConnectionStateMonitor() {
        if (connectionStateMonitor == null) {
            connectionStateMonitor = new WiFiConnectionStateMonitor(application);
        }
        return connectionStateMonitor.getConnectionStateMonitor();
    }

    public Object getNetworkCapability() {
        return connectionStateMonitor != null ? connectionStateMonitor.getNetworkCapability() : null;
    }

    public void setNetworkCapability() {
        connectionStateMonitor.setNetworkCapability();
    }

    public void unRegisterConnectionStateMonitor() {
        if (connectionStateMonitor != null) {
            connectionStateMonitor.unregisterCallbacks();
        }
    }

    public void removeConnectionStateMonitor() {
        try {
            if (connectionStateMonitor != null) {
                connectionStateMonitor.unregisterCallbacks();
                application.unregisterReceiver(wifiBroadCastReceiver);
                connectionStateMonitor = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopObserveWiFiOnOffState() {
        if (wifiBroadCastReceiver != null)
            application.unregisterReceiver(wifiBroadCastReceiver);
    }

    public void resetWifiState() {
        isCameraAvailableState = CAMERA_NOT_AVAILABLE;
    }

    public MutableLiveData<WiFiNetworkCallback> getNetworkCallbackMutableLiveData() {
        return connectionStateMonitor.getNetworkCallbackMutableLiveData();
    }
}
