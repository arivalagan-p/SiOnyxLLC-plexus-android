package com.sionyx.plexus.ui.api.interfaces;

public interface CloudInterface {
    void onSuccess(String state, String msg);
    void onFailure(String error);
}