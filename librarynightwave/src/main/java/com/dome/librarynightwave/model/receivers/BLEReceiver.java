package com.dome.librarynightwave.model.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.util.Objects;

public class BLEReceiver extends BroadcastReceiver {
    String TAG = "BLEReceiver";
    private final MutableLiveData<Boolean> isBluetoothOn = new MutableLiveData<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Objects.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.e(TAG, "STATE_OFF");
                    isBluetoothOn.postValue(false);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.e(TAG, "STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.e(TAG, "STATE_ON");
                    isBluetoothOn.postValue(true);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.e(TAG, "STATE_TURNING_ON");
                    break;
            }
        }
    }
    public MutableLiveData<Boolean> isBluetoothOnOffLiveState() {
        return isBluetoothOn;
    }

}
