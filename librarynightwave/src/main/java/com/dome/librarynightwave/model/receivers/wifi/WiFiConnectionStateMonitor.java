package com.dome.librarynightwave.model.receivers.wifi;

import static android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiConnectionStateMonitor.NetworkRegisteredState.NETWORK_REGISTERED_STATE_NOT_REGISTERED;
import static com.dome.librarynightwave.model.receivers.wifi.WiFiNetworkCallback.networkCapability;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dome.librarynightwave.model.repository.pojo.NetworkChange;
import com.dome.librarynightwave.utils.Event;

public class WiFiConnectionStateMonitor extends LiveData<Event<NetworkChange>> {
    private static final String TAG = "ConnectionStateMonitor";
    public WiFiNetworkCallback networkCallback;
    private final ConnectivityManager connectivityManager;
    protected WiFiConnectionStateMonitor mConnectionStateMonitor;
    public static int wifiRequestCount = 0;
    public static int bleDiscoverRequestCount = 0;
    private final MutableLiveData<WiFiNetworkCallback> networkCallbackMutableLiveData = new MutableLiveData<>();

    public static boolean isHomeNetworkRequest =  false;

    public enum NetworkRegisteredState {
        NETWORK_REGISTERED_STATE_REGISTERED,
        NETWORK_REGISTERED_STATE_NOT_REGISTERED
    }

    public static NetworkRegisteredState networkRegisteredStatus = NETWORK_REGISTERED_STATE_NOT_REGISTERED;

    public WiFiConnectionStateMonitor getConnectionStateMonitor() {
        return mConnectionStateMonitor;
    }

    public MutableLiveData<WiFiNetworkCallback> getNetworkCallbackMutableLiveData() {
        return networkCallbackMutableLiveData;
    }

    public NetworkCapabilities getNetworkCapability() {
        return networkCapability;
    }

    public void setNetworkCapability() {
        networkCapability = null;
    }

    public WiFiConnectionStateMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            networkCallback = new WiFiNetworkCallback(this, context, mWifiManager, connectivityManager, FLAG_INCLUDE_LOCATION_INFO);
        } else {
            networkCallback = new WiFiNetworkCallback(this, context, mWifiManager, connectivityManager);
        }
        networkCallbackMutableLiveData.postValue(networkCallback);
    }

    @Override
    protected void onActive() {
        super.onActive();
        Log.e(TAG, "onActive");
        registerCallbacks();
    }

    @Override
    public void onInactive() {
        super.onInactive();
        Log.e(TAG, "onInactive");
        unregisterCallbacks();
    }

    public void registerCallbacks() {
        Log.e(TAG, "registerCallbacks()");
        NetworkRequest networkRequest ;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            networkRequest =  new NetworkRequest.Builder()
//                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
//                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
//                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                    .build();

            networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
        }else {
            networkRequest =  new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build();
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        networkRegisteredStatus = NetworkRegisteredState.NETWORK_REGISTERED_STATE_REGISTERED;
        isHomeNetworkRequest = true;
    }

    public void unregisterCallbacks() {
        Log.e(TAG, "unregisterCallbacks: " + (connectivityManager != null));
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkRegisteredStatus = NetworkRegisteredState.NETWORK_REGISTERED_STATE_NOT_REGISTERED;
        }
    }

    public void post(NetworkChange networkChange) {
        mConnectionStateMonitor.postValue(new Event<>(networkChange));
    }

    public void setConnectionStateMonitor(WiFiConnectionStateMonitor connectionStateMonitor) {
        this.mConnectionStateMonitor = connectionStateMonitor;
    }
}
