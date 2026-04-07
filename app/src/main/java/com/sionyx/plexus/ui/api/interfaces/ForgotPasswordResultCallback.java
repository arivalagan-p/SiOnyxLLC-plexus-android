package com.sionyx.plexus.ui.api.interfaces;

public interface ForgotPasswordResultCallback {
    void onSuccess(String forgotPasswordResponse, int statusCode);
    void onFailure(String error);
}
