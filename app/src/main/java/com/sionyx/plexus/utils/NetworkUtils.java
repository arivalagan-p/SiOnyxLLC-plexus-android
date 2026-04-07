package com.sionyx.plexus.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class NetworkUtils {
    private static final Executor executor = Executors.newSingleThreadExecutor();
    public static void pingServer(PingCallback callback) {
        executor.execute(() -> {
            boolean isSuccessful = pingServer();
            if (callback != null) {
                callback.onPingResult(isSuccessful);
            }
        });
    }

    private static boolean pingServer() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return address.isReachable(5000); // 5000 milliseconds timeout
        } catch (IOException e) {
            return false;
        }
    }

    public interface PingCallback {
        void onPingResult(boolean isSuccessful);
    }
}
