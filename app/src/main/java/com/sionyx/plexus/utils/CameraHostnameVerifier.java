package com.sionyx.plexus.utils;

import android.util.Log;

import com.dome.librarynightwave.utils.Constants;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class CameraHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        try {

            String cameraIp = Constants.getCameraDynamicIpAddress();

            Log.w("CameraHostnameVerifier", "Verifying: " + hostname + " vs cameraIp= " + cameraIp);


            if (hostname.equals(cameraIp) || hostname.equalsIgnoreCase("nightwave.local") ||
                    hostname.equalsIgnoreCase("nightwave.local-1")) {
                Log.i("CameraHostnameVerifier", " Verified dynamic camera IP: " + hostname);
                return true;
            }

            // Otherwise, fall back to default verification
            javax.net.ssl.HostnameVerifier defaultVerifier =
                    javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();
            return defaultVerifier.verify(hostname, session);

        } catch (Exception e) {
            Log.e("CameraHostnameVerifier", "Verification failed: " + e.getMessage());
            return false;
        }
    }
}

