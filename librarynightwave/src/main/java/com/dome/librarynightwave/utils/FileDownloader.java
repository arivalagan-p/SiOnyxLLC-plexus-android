package com.dome.librarynightwave.utils;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class FileDownloader {
    private static final String TAG = "FileDownloader";

    private static final int MEGABYTE =256;

    public static void downloadFile(String fileUrl, File directory) {
        try {
            Log.v(TAG, "downloadFile() invoked ");
            Log.v(TAG, "downloadFile() fileUrl " + fileUrl);
            Log.v(TAG, "downloadFile() directory " + directory);

            URL url = new URL(fileUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(directory);
            int totalSize = urlConnection.getContentLength();

            byte[] buffer = new byte[MEGABYTE];
            int bufferLength = 0;
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bufferLength);
            }
            fileOutputStream.close();
            Log.v(TAG, "downloadFile() completed ");

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile() error" + e.getMessage());
            Log.e(TAG, "downloadFile() error" + Arrays.toString(e.getStackTrace()));
        }
    }
}
