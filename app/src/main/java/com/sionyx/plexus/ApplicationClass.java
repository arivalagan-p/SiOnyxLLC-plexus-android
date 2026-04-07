package com.sionyx.plexus;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import com.sionyx.plexus.utils.Foreground;

public class ApplicationClass extends Application {
    public static Context appContext;
    @Override
    public void onCreate() {
        super.onCreate();
        Foreground.init(this);
        appContext = getApplicationContext();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onTerminate() {
       // Constants.hasNightWaveCamera = false;
        super.onTerminate();
    }
}
