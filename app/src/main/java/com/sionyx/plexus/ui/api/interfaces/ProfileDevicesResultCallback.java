package com.sionyx.plexus.ui.api.interfaces;

import com.sionyx.plexus.ui.api.responseModel.GetProfileResponse;

public interface ProfileDevicesResultCallback {
    void onSuccess(GetProfileResponse getProfileResponse);
    void onFailure(String error, String message);
}
