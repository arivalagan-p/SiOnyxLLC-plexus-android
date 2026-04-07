package com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback;

public interface CameraResetCallback {

    void onSuccess(boolean isReset, int responseCode);

    void onFailure(String message);
}
