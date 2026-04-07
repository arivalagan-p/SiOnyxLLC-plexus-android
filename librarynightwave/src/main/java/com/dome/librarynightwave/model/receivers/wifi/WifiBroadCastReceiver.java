package com.dome.librarynightwave.model.receivers.wifi;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

public class WifiBroadCastReceiver extends BroadcastReceiver {
    private final String TAG = "WifiBroadCastReceiver";
    private WifiManager wifiManager;
    private final MutableLiveData<Boolean> isWiFiEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isWiFiOn = new MutableLiveData<>();

    public WifiBroadCastReceiver(Application application) {
        if(wifiManager == null)
            wifiManager = (WifiManager) application.getSystemService(Context.WIFI_SERVICE);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    int newState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    if (newState == WifiManager.WIFI_STATE_ENABLED) {
                        isWiFiOn.setValue(true);
                        Log.e(TAG, "WIFI_STATE_ENABLED");
                    }else if (newState == WifiManager.WIFI_STATE_DISABLED) {
                        isWiFiOn.setValue(false);
                        Log.e(TAG, "WIFI_STATE_DISABLED");
                    }
                    break;
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    Log.e(TAG, "SCAN_RESULTS_AVAILABLE_ACTION");
            }
        }
    }

    public MutableLiveData<Boolean> isWiFiEnabled() {
        Log.e(TAG, "isWiFiEnabled: Called");
        if(wifiManager.isWifiEnabled())
            isWiFiEnabled.setValue(true);
        else
            isWiFiEnabled.setValue(false);
        return isWiFiEnabled;
    }

    public MutableLiveData<Boolean> isWiFiOnOffLiveState() {
        return isWiFiOn;
    }
}
