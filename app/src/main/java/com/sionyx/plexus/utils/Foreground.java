package com.sionyx.plexus.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Foreground implements Application.ActivityLifecycleCallbacks {

    public static final long CHECK_DELAY = 500;
    public static final String TAG = Foreground.class.getName();

    public interface Listener {

        void onBecameForeground();

        void onBecameBackground();

        void onBecameDestoryed();

    }

    private static Foreground instance;

    private boolean foreground = false;
    private boolean paused = true;
    private final boolean destroyed = false;
    private final Handler handler = new Handler();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private Runnable check;

    /**
     * Its not strictly necessary to use this method - _usually_ invoking
     * get with a Context gives us a path to retrieve the Application and
     * initialise, but sometimes (e.g. in test harness) the ApplicationContext
     * is != the Application, and the docs make no guarantees.
     *
     * @param application
     * @return an initialised Foreground instance
     */
    public static Foreground init(Application application){
        if (instance == null) {
            instance = new Foreground();
            application.registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    public static Foreground get(Application application){
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    public static Foreground get(Context ctx){
        if (instance == null) {
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                init((Application)appCtx);
            }
            throw new IllegalStateException(
                    "Foreground is not initialised and " +
                            "cannot obtain the Application object");
        }
        return instance;
    }

    public static Foreground get(){
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/get");
        }
        return instance;
    }

    public boolean isForeground(){
        return foreground;
    }

    public boolean isBackground(){
        return !foreground;
    }

    public void addListener(Listener listener){
        listeners.add(listener);
    }

    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Log.e("onActivity","Resumed");

        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground){
            Log.i(TAG, "went foreground");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception exc) {
                    Log.e(TAG, "Listener threw exception!", exc);
                }
            }
        } else {
            Log.i(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.e("onActivity","Paused");
        paused = true;
        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = () -> {
            if (foreground && paused /*&& !destroyed*/) {
                foreground = false;
                Log.i(TAG, "went background");
                for (Listener l : listeners) {
                    try {
                        l.onBecameBackground();
                    } catch (Exception exc) {
                        Log.e(TAG, "Listener threw exception!", exc);
                    }
                }
            } else {
                Log.i(TAG, "still foreground");
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Log.e("onActivity","Stopped");
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, Bundle outState) {
        Log.e("onActivity","SaveInstanceState");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.e("onActivity ","Destroyed");

//        destroyed = true;
//        for (Listener l : listeners) {
//            try {
//                l.onBecameDestoryed();
//            } catch (Exception exc) {
//                Log.e(TAG, "Listener threw exception!", exc);
//            }
//        }
    }
}