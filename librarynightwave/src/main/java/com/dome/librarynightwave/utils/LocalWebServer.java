package com.dome.librarynightwave.utils;


import static com.dome.librarynightwave.model.repository.localserver.NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED;
import static com.dome.librarynightwave.model.repository.localserver.NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.dome.librarynightwave.model.repository.localserver.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;


public class LocalWebServer extends NanoHTTPD {
    public static String fileName = "";
    private final Context mContext;
    private final String TAG = "LocalWebServer";
    private Response response;

    public LocalWebServer(int port, String fileName, Context context, Application application) {
        super(port,context,application);
        mContext = context;
        LocalWebServer.fileName = fileName;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() != NanoHTTPD.Method.GET) {
            response =  newFixedLengthResponse(METHOD_NOT_ALLOWED, MIME_PLAINTEXT, null);
        }else {
            String uri = session.getUri();
            Log.e(TAG, "serve: " + uri);
            if (uri.contains(fileName)) {
                InputStream assetInputStream = null;
                int available = 0;
                try {
                    assetInputStream = mContext.getAssets().open(fileName);
                    available = assetInputStream.available();

                    Log.e(TAG, "serve: started "+available);
                    String mime = NanoHTTPD.getMimeTypeForFile(fileName);
                    response = newFixedLengthResponse(Response.Status.OK, mime, assetInputStream, available);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    Log.e(TAG, "serve: done");
                } catch (IOException e) {
                    Log.e(TAG, "serve: " + e.getLocalizedMessage());
                    e.printStackTrace();
                    response = newFixedLengthResponse(SERVICE_UNAVAILABLE, MIME_PLAINTEXT, null);
                }
            }
        }

//        InputStream assetInputStream = null;
//        int available = 0;
//        try {
//            assetInputStream = mContext.getAssets().open(fileName);
//            available = assetInputStream.available();
//
//            Log.e(TAG, "serve: started "+available);
//            String mime = NanoHTTPD.getMimeTypeForFile(fileName);
//            response = newFixedLengthResponse(Response.Status.OK, mime, assetInputStream, available);
//            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
//            Log.e(TAG, "serve: done");
//        } catch (IOException e) {
//            Log.e(TAG, "serve: " + e.getLocalizedMessage());
//            e.printStackTrace();
//            response = newFixedLengthResponse(SERVICE_UNAVAILABLE, MIME_PLAINTEXT, null);
//        }
        return response;
    }
}
