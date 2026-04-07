package com.sionyx.plexus.ui.api.interfaces;

public interface ProductUploadResultCallback {
    void onSuccess(String message);

    void onFailure(String error, String message);
}
