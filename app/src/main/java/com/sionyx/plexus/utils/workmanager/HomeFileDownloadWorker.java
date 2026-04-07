package com.sionyx.plexus.utils.workmanager;

import static com.dome.librarynightwave.utils.Constants.isSDK14;
import static com.dome.librarynightwave.utils.Constants.isSDK15;
import static com.dome.librarynightwave.utils.Constants.isSDK16AndAbove;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sionyx.plexus.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class HomeFileDownloadWorker extends Worker {
    public static String TAG = "DownloadWorker";
    public static String NOTIFICATION_CHANNEL_ID = "download_channel_id";
    public static String NOTIFICATION_CHANNEL_NAME = "download_to_gallery";
    public static String FILE_DOWNLOAD = "File Download";
    public static String FILE_PATH = "filePath";
    public static String DESTINATION_FILE_PATH = "destinationFilePath";
    public static String IS_IMAGE = "isImage";
    public static String NOTIFICATION_ID = "Notification_id";
    public static int NOTIFICATION_ID_ALL = 1;
    public static int NOTIFICATION_ID_IMG = 2;
    public static int NOTIFICATION_ID_VIDEO = 3;
    public HomeFileDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createNotificationChannel(getApplicationContext());
        boolean isImage = getInputData().getBoolean(IS_IMAGE, false);
        String filePath = getInputData().getString(FILE_PATH);
        String destinationPath = getInputData().getString(DESTINATION_FILE_PATH);
        int notificationId = getInputData().getInt(NOTIFICATION_ID, 0);
        Log.e(TAG, "doWork: " + filePath);

        NotificationCompat.Builder notificationBuilder = getNotificationBuilder();
        ForegroundInfo foregroundInfo ;
        if (isSDK14() ||  isSDK15() || isSDK16AndAbove()) {
            foregroundInfo = new ForegroundInfo(notificationId, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }else {
            foregroundInfo = new ForegroundInfo(notificationId, notificationBuilder.build());
        }
        setForegroundAsync(foregroundInfo);

        File sourceFile = null;
        if (filePath != null) {
            sourceFile = new File(filePath);
        }
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        ContentValues contentValues = getContentValues(isImage, sourceFile, destinationPath);

        Uri mediaUri = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // Open input stream for the source file
            if (sourceFile != null) {
                inputStream = Files.newInputStream(sourceFile.toPath());
            }

            if (isImage) {
                // Insert the video into MediaStore
                mediaUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                // Insert the video into MediaStore
                mediaUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);

            }
            if (mediaUri == null) {
                Log.e(TAG, "Failed to insert media into MediaStore");
                return Result.failure();
            }
            // Open output stream for the video URI
            outputStream = contentResolver.openOutputStream(mediaUri);
            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream for video");
                return Result.failure();
            }

            // Copy the data from the input stream to the output stream
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.d(TAG, "Media copied to public folder: " + mediaUri);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to copy video: " + e.getMessage());
            return Result.failure();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to close streams: " + e.getMessage());
            }
        }
        Data outputData = new Data.Builder()
                .putInt("resultKey", 1)
                .build();
        return Result.success(outputData);
    }

    @NonNull
    private static ContentValues getContentValues(boolean isImage, File sourceFile, String destinationPath) {
        ContentValues contentValues = new ContentValues();
        if (isImage) {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.getName());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + destinationPath);
        } else {
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.getName());
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + File.separator + destinationPath);
        }
        return contentValues;
    }

    @NonNull
    private NotificationCompat.Builder getNotificationBuilder() {
        return new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setContentTitle(FILE_DOWNLOAD)
                .setContentText("Downloading file...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSmallIcon(R.drawable.image_sionyx);
    }

    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}

