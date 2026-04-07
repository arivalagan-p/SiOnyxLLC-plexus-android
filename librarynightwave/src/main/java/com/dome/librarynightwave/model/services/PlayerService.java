package com.dome.librarynightwave.model.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PlayerService extends Service {
    private static final String TAG = "RtspPlayerService";
    public static final String CHANNEL_ID = "RTSP_PLAYER_CHANNEL";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_START_SERVICE = "com.jasmin.app.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.jasmin.app.STOP_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received. Action: " + (intent != null ? intent.getAction() : "null"));

        if (intent == null) {
            Log.w(TAG, "Received null intent.  Returning START_STICKY.");
            return START_STICKY;
        }

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_START_SERVICE:
                    Notification notification = buildNotification();

                    if (isSDK14() || isSDK15() || isSDK16AndAbove()) {
                        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);//Android 14 and 15
                    } else {
                        startForeground(NOTIFICATION_ID, notification, 0);
                    }
//                    startForeground(NOTIFICATION_ID, notification, 0);
                    break;
                case ACTION_STOP_SERVICE:
                    stopService();
                    break;
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        }
        return START_STICKY;
    }

    private void stopService() {
        Log.d(TAG, "Stopping service");
        stopForeground(true); // Remove notification
        stopSelf();             // Stop the service.
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "RTSP Player Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, getApplicationContext().getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RTSP Streaming")
                .setContentText("Service is running")  // General message.
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(pendingIntent);
        builder.setChannelId(CHANNEL_ID);
        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @ChecksSdkIntAtLeast(api = 34)
    public static boolean isSDK14() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }

    @ChecksSdkIntAtLeast(api = 35)
    public static boolean isSDK15() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.VANILLA_ICE_CREAM;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    public static boolean isSDK16AndAbove(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA;
    }
}