package com.sionyx.plexus.ui.api.interfaces;

import com.sionyx.plexus.ui.api.responseModel.ProfileDevices;

import java.util.ArrayList;

public interface ProfileInterface {
    void onSuccess(String message, ArrayList<ProfileDevices> profileDevicesArrayList);
    void onFailure(String error, String message);
}
