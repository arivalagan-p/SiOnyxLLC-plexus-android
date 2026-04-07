package com.dome.librarynightwave.model.receivers.wifi;

import static com.dome.librarynightwave.model.services.TCPCommunicationService.lastCommandSentTime;
import static com.dome.librarynightwave.utils.Constants.DEFAULT_IP_ADDRESS;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING1;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING2;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING3;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING4;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING5;
import static com.dome.librarynightwave.utils.Constants.FILTER_STRING6;
import static com.dome.librarynightwave.utils.Constants.STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_CONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_DISCONNECTED;
import static com.dome.librarynightwave.utils.Constants.WIFI_STATE_NONE;
import static com.dome.librarynightwave.utils.Constants.mState;
import static com.dome.librarynightwave.utils.Constants.mWifiState;
import static com.dome.librarynightwave.utils.Constants.currentCameraSsid;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.dome.librarynightwave.model.repository.pojo.NetworkChange;
import com.dome.librarynightwave.utils.Constants;

import java.io.IOException;
import java.net.InetAddress;

public class WiFiNetworkCallback extends ConnectivityManager.NetworkCallback {
    private static final String TAG = "NetworkCallback";
    protected WiFiConnectionStateMonitor mConnectionStateMonitor;
    private final ConnectivityManager connectivityManager;
    private final WifiManager mWifiManager;
    private final Context mContext;

    private int connectedNetworkId = -1;
    public static final int CAMERA_NOT_AVAILABLE = 0;
    public static final int CAMERA_AVAILABLE_PENDING_CONFIRMATION = 1;
    public static final int CAMERA_AVAILABLE = 2;
    public static int isCameraAvailableState = CAMERA_NOT_AVAILABLE;
    public static NetworkCapabilities networkCapability = null;
    public static boolean isUnknownSSIDAvailable = false;
    private boolean isHomeNetworkConnected = false;
    private Network homeNetwork;

    public WiFiNetworkCallback(WiFiConnectionStateMonitor connectionStateMonitor, Context context, WifiManager mWifiManager, ConnectivityManager connectivityManager) {
        mContext = context;
        mConnectionStateMonitor = connectionStateMonitor;
        mConnectionStateMonitor.setConnectionStateMonitor(connectionStateMonitor);
        this.mWifiManager = mWifiManager;
        this.connectivityManager = connectivityManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    public WiFiNetworkCallback(WiFiConnectionStateMonitor connectionStateMonitor, Context context, WifiManager mWifiManager, ConnectivityManager connectivityManager, int flagIncludeLocationInfo) {
        super(flagIncludeLocationInfo);
        mContext = context;
        mConnectionStateMonitor = connectionStateMonitor;
        mConnectionStateMonitor.setConnectionStateMonitor(connectionStateMonitor);
        this.mWifiManager = mWifiManager;
        this.connectivityManager = connectivityManager;

    }

    @Override
    public void onAvailable(@NonNull Network network) {
        Log.e(TAG, "onAvailable: " + network);
        if (isCameraAvailableState == CAMERA_NOT_AVAILABLE) {
            isCameraAvailableState = CAMERA_AVAILABLE_PENDING_CONFIRMATION;
        }
    }

    @Override
    public void onUnavailable() {
        super.onUnavailable();
        Log.d(TAG, "onUnavailable: ");
        postTimeOut();
    }

    @Override
    public void onLost(@NonNull Network network) {
        Log.e(TAG, "onLost: " + network);
        int disconnectedNetworkId = Integer.parseInt(network.toString());
        if (connectedNetworkId == disconnectedNetworkId) {
            mWifiState = WIFI_STATE_DISCONNECTED;
            postWiFiDisconnected(network);
        } else {
            Log.e(TAG, "onLost: Non-Camera");
        }
    }

    @Override
    public void onLosing(@NonNull Network network, int maxMsToLive) {
        super.onLosing(network, maxMsToLive);
        Log.d(TAG, "onLosing: " + mWifiManager.getConnectionInfo().getSSID() + mWifiManager.getConnectionInfo().getBSSID());
    }

    @Override
    public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
        super.onLinkPropertiesChanged(network, linkProperties);
        Log.d(TAG, "onLinkPropertiesChanged: " + linkProperties);

    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);
        try {
            if (!InetAddress.getByName(DEFAULT_IP_ADDRESS).isReachable(1000)) {
                if (connectivityManager != null) {
                    connectivityManager.bindProcessToNetwork(network);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            int signalStrength = networkCapabilities.getSignalStrength();
            networkCapability = networkCapabilities;

            boolean netTrusted = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);
            boolean netForeground = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND);
            boolean netValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
            boolean notSuspended = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);
            int linkDownstreamBandwidthKbps = networkCapabilities.getLinkDownstreamBandwidthKbps();
            int linkUpstreamBandwidthKbps = networkCapabilities.getLinkUpstreamBandwidthKbps();

            if (isSDKBelow12x()) {
                String ssid = mWifiManager.getConnectionInfo().getSSID();
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                if (isCamera(ssid) && isCameraAvailableState == CAMERA_AVAILABLE_PENDING_CONFIRMATION) {
                    connectedNetworkId = Integer.parseInt(network.toString());
                    isCameraAvailableState = CAMERA_AVAILABLE;
                    NetworkChange networkChange = new NetworkChange();
                    networkChange.setNetwork(network);
                    networkChange.setAvailable(true);
                    networkChange.setConnectedSSID(ssid);
                    postConnected(networkChange);
                }
                Log.d(TAG, "TRANSPORT_WIFI " + signalStrength
                        + " SSID: " + ssid
                        + " Network ID: " + network
                        + " NET_CAPABILITY_TRUSTED: " + netTrusted
                        + " NET_CAPABILITY_FOREGROUND: " + netForeground
                        + " NET_CAPABILITY_NOT_CONGESTED: " + netValidated
                        + " NET_CAPABILITY_NOT_SUSPENDED: " + notSuspended
                        + " getLinkDownstreamBandwidthKbps: " + linkDownstreamBandwidthKbps
                        + " getLinkUpstreamBandwidthKbps: " + linkUpstreamBandwidthKbps);
            } else {
                final TransportInfo transportInfo = networkCapabilities.getTransportInfo();
                if (transportInfo instanceof WifiInfo) {
                    final WifiInfo wifiInfo = (WifiInfo) transportInfo;
                    String ssid = wifiInfo.getSSID();
                    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    if (!ssid.equalsIgnoreCase("<unknown ssid>")) {
                        Log.e(TAG, "onCapabilitiesChanged: " + isCameraAvailableState);
                        /*long currentMill = System.currentTimeMillis();
                        long timeDifference = currentMill - lastCommandSentTime;
                        if (lastCommandSentTime != -1 && timeDifference >= 15000 && isCamera(ssid) && isCameraAvailableState == CAMERA_AVAILABLE) {
                            Log.e(TAG, "onCapabilitiesChanged: " + isCameraAvailableState + " CAMERA_AVAILABLE but Socket may be disconnected due to no response");
                            lastCommandSentTime = System.currentTimeMillis();
                            NetworkChange networkChange = new NetworkChange();
                            networkChange.setNetwork(network);
                            networkChange.setDisconnectSocket(true);
                            networkChange.setConnectedSSID(ssid);
                            postConnected(networkChange);
                            mConnectionStateMonitor.post(networkChange);

                            final String SSID = ssid;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                connectedNetworkId = Integer.parseInt(network.toString());
                                isCameraAvailableState = CAMERA_AVAILABLE;
                                NetworkChange networkChange1 = new NetworkChange();
                                networkChange1.setNetwork(network);
                                networkChange1.setAvailable(true);
                                networkChange1.setConnectedSSID(SSID);
                                postConnected(networkChange1);
                            }, 2000);

                        } else*/
                        if (isCamera(ssid) && isCameraAvailableState == CAMERA_AVAILABLE_PENDING_CONFIRMATION) {
                            Log.e(TAG, "onCapabilitiesChanged: " + isCameraAvailableState + " CAMERA_AVAILABLE_PENDING_CONFIRMATION");
                            connectedNetworkId = Integer.parseInt(network.toString());
                            isCameraAvailableState = CAMERA_AVAILABLE;
                            NetworkChange networkChange = new NetworkChange();
                            networkChange.setNetwork(network);
                            networkChange.setAvailable(true);
                            networkChange.setConnectedSSID(ssid);
                            postConnected(networkChange);
                        } else {
                            isHomeNetworkConnected = true;
                            homeNetwork = network;
                        }
                    } else {
                        //Handle Unknown SSID
                        if (isHomeNetworkConnected) {
                            isUnknownSSIDAvailable = network != homeNetwork;
                        } else {
                            isUnknownSSIDAvailable = true;
                        }
                        Log.e(TAG, "onCapabilitiesChanged: UNKNOWN SSID");
                    }

                    Log.d(TAG, "TRANSPORT_WIFI " + signalStrength
                            + " SSID: " + ssid
                            + " Network ID: " + network
                            + " NET_CAPABILITY_TRUSTED: " + netTrusted
                            + " NET_CAPABILITY_FOREGROUND: " + netForeground
                            + " NET_CAPABILITY_NOT_CONGESTED: " + netValidated
                            + " NET_CAPABILITY_NOT_SUSPENDED: " + notSuspended
                            + " getLinkDownstreamBandwidthKbps: " + linkDownstreamBandwidthKbps
                            + " getLinkUpstreamBandwidthKbps: " + linkUpstreamBandwidthKbps);

                } else {
                    Log.d(TAG, "TRANSPORT_WIFI ELSE " + signalStrength + " Network " + network
                            + " NET_CAPABILITY_TRUSTED: " + netTrusted + " NET_CAPABILITY_FOREGROUND: " + netForeground + " NET_CAPABILITY_NOT_CONGESTED: " + netValidated
                            + " getLinkDownstreamBandwidthKbps: " + linkDownstreamBandwidthKbps + " getLinkUpstreamBandwidthKbps: " + linkUpstreamBandwidthKbps);
                }
            }
        } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.d("onCapabilitiesChanged ", "TRANSPORT_CELLULAR");
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                Log.d("onCapabilitiesChanged ", "INTERNET AVAILABLE IN MOBILE DATA");
            } else {
                Log.d("onCapabilitiesChanged ", "INTERNET NOT AVAILABLE IN MOBILE DATA");
            }
        }
    }

    private void postConnected(NetworkChange networkChange) {
        if (connectivityManager != null) {
            connectivityManager.bindProcessToNetwork(networkChange.getNetwork());
        }
        Constants.mWifiState = WIFI_STATE_CONNECTED;
        mConnectionStateMonitor.post(networkChange);
    }

    private void postWiFiDisconnected(Network network) {
        if (connectivityManager != null) {
            connectivityManager.bindProcessToNetwork(null);
            networkCapability = null;
        }
        isCameraAvailableState = CAMERA_NOT_AVAILABLE;
        Constants.mWifiState = WIFI_STATE_DISCONNECTED;
        NetworkChange networkChange = new NetworkChange();
        networkChange.setNetwork(network);
        networkChange.setAvailable(false);

        new Handler().postDelayed(() -> {
            if (isCameraAvailableState != CAMERA_AVAILABLE) {
                mConnectionStateMonitor.post(networkChange);
            }
        }, 2000);

    }

    private void postTimeOut() {
        Constants.mWifiState = WIFI_STATE_NONE;
        NetworkChange networkChange = new NetworkChange();
        networkChange.setNetwork(null);
        networkChange.setAvailable(false);
        networkChange.setUnableToConnect("TIME_OUT");
        mConnectionStateMonitor.post(networkChange);
    }

    private boolean isCamera(String ssid) {
        Log.e(TAG, "isCamera: " + ssid);
        return ssid.contains(FILTER_STRING1)
                || ssid.contains(FILTER_STRING2)
                || ssid.contains(FILTER_STRING3)
                || ssid.contains(FILTER_STRING4)
                || ssid.contains(FILTER_STRING5)
                || ssid.contains(FILTER_STRING6);
    }

    public static boolean isSDKBelow12x() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2;
    }
}
