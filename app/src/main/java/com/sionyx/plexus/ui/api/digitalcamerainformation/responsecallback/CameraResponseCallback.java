package com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback;

public interface CameraResponseCallback {

    void onSuccess(String responseStr, int code);

    void onFailure(String message);
}
