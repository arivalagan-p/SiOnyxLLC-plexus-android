package com.sionyx.plexus.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.dome.librarynightwave.utils.Constants;
import com.dome.librarynightwave.viewmodel.BleWiFiViewModel;
import com.dome.librarynightwave.viewmodel.TCPConnectionViewModel;
import com.sionyx.plexus.ui.cameramenu.model.DigitalCameraInfoViewModel;
import com.sionyx.plexus.ui.home.HomeViewModel;
import com.sionyx.plexus.utils.Foreground;

public abstract class BaseActivity extends AppCompatActivity {
    String TAG = "BaseActivity";
    protected ConnectivityManager connectivityManager;
    protected WifiManager mWifiManager;
    public BleWiFiViewModel viewModel;
    public DigitalCameraInfoViewModel digitalCameraInfoViewModel;
    public TCPConnectionViewModel tcpConnectionViewModel;
    public HomeViewModel homeViewModel;
    private LifecycleOwner lifecycleOwner;

    public abstract void onBluetoothOnOffState(boolean isOn);
    public abstract void onWiFiOnOffState(boolean isOn);
    public abstract void onSocketConnectionState(int state);
    public abstract void onBecameForeground();
    public abstract void onBecameBackground();
    public abstract void onBecameDestoryed();


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleOwner = this;
        Foreground.get(this).addListener(myListener);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        viewModel = new ViewModelProvider(this).get(BleWiFiViewModel.class);
        tcpConnectionViewModel = new ViewModelProvider(this).get(TCPConnectionViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.startObserveBleOnOffState().observeForever(observer);
        viewModel.startObserveWiFiOnOffState().observeForever(observerWifi);

        tcpConnectionViewModel.isSocketConnected().observe(this, mState -> {
            if (mState == Constants.STATE_CONNECTED) {
                onSocketConnectionState(Constants.STATE_CONNECTED);
            } else if (mState == Constants.STATE_FAILED) {
                onSocketConnectionState(Constants.STATE_FAILED);
            } else if (mState == Constants.STATE_DISCONNECTED) {
                onSocketConnectionState(Constants.STATE_DISCONNECTED);
            }
        });

    }

    Observer<Boolean> observer = aBoolean -> {
        Log.e(TAG, "startObserveBleOnOffState: " + aBoolean);
        if (aBoolean != null) {
            onBluetoothOnOffState(aBoolean);
        }
    };
    Observer<Boolean> observerWifi = new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean aBoolean) {
            if (aBoolean != null) {
                if (homeViewModel.isWiFiStateFirstTime()) {
                    homeViewModel.setWiFiStateFirstTime(false);
                } else {
                    onWiFiOnOffState(aBoolean);
                }
            }
        }
    };

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.startObserveBleOnOffState().removeObservers(lifecycleOwner);
        viewModel.startObserveBleOnOffState().removeObserver(observer);

        viewModel.startObserveWiFiOnOffState().removeObservers(lifecycleOwner);
        viewModel.startObserveWiFiOnOffState().removeObserver(observerWifi);

        homeViewModel.setBleStateFirstTime(true);
        homeViewModel.setWiFiStateFirstTime(true);

        Foreground.get(this).removeListener(myListener);
    }


    Foreground.Listener myListener = new Foreground.Listener() {

        public void onBecameForeground() {
            Log.e(TAG, "onBecameForeground" + " " );
            BaseActivity.this.onBecameForeground();

        }

        public void onBecameBackground() {
            Log.e(TAG, "onBecameBackground" + " " );
            BaseActivity.this.onBecameBackground();
        }

        @Override
        public void onBecameDestoryed() {
            Log.e(TAG, "Destroyed");
            BaseActivity.this.onBecameDestoryed();
        }
    };
}
