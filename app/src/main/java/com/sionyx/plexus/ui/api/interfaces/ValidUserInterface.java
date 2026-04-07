package com.sionyx.plexus.ui.api.interfaces;

public interface ValidUserInterface {
    void onSuccess(int statusCode, String msg);

    void onFailure(String error);
}
