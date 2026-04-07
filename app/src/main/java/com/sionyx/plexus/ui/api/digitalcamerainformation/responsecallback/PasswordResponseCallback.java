package com.sionyx.plexus.ui.api.digitalcamerainformation.responsecallback;

public interface PasswordResponseCallback {

    void onSuccess(String password, int responseCode);//pwd

    void onFailure(String message);
}
