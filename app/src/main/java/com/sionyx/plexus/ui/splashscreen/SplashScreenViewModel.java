package com.sionyx.plexus.ui.splashscreen;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

public class SplashScreenViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle; // for this device orientation changed then navigate to other fragment issue handled
    private final MutableLiveData<String> data;
    private final MutableLiveData<String> loginState;
    private final MutableLiveData<String> forgotPasswordState;
    private final MutableLiveData<String> changeNewPasswordState;
    private final MutableLiveData<String> qrCodeSaveState;

    public SplashScreenViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        if (!savedStateHandle.contains("data")) {
            savedStateHandle.set("data", ""); // Initialize data if not present
        }

        if (!savedStateHandle.contains("login")) {
            savedStateHandle.set("login", ""); // Initialize data if not present
        }

        if (!savedStateHandle.contains("forgotPassword")) {
            savedStateHandle.set("forgotPassword", ""); // Initialize data if not present
        }

        if (!savedStateHandle.contains("newPasswordChange")) {
            savedStateHandle.set("newPasswordChange", ""); // Initialize data if not present
        }

        if (!savedStateHandle.contains("qrCodeSaveState")) {
            savedStateHandle.set("qrCodeSaveState", ""); // Initialize data if not present
        }

        data = savedStateHandle.getLiveData("data");
        loginState = savedStateHandle.getLiveData("login");
        forgotPasswordState = savedStateHandle.getLiveData("forgotPassword");
        changeNewPasswordState = savedStateHandle.getLiveData("newPasswordChange");
        qrCodeSaveState = savedStateHandle.getLiveData("qrCodeSaveState");
    }

    public MutableLiveData<String> getData() {
        return data;
    }

    public MutableLiveData<String> getLoginState() {
        return loginState;
    }

    public MutableLiveData<String> getForgotPasswordState() {
        return forgotPasswordState;
    }

    public MutableLiveData<String> getNewPasswordChangeState() {
        return changeNewPasswordState;
    }

    public MutableLiveData<String> getQrCodeSaveState() {
        return qrCodeSaveState;
    }

    public void setData(String newData) {
        savedStateHandle.set("data", newData);
    }

    public void setLoginState(String loginState) {
        savedStateHandle.set("login", loginState);
    }

    public void setForgotPasswordState(String forgotPasswordState) {
        savedStateHandle.set("forgotPassword", forgotPasswordState);
    }

    public void setNewPasswordChangeState(String newPasswordChangeState) {
        savedStateHandle.set("newPasswordChange", newPasswordChangeState);
    }

    public void setQrCodeSaveState(String qrCodeSaveState) {
        savedStateHandle.set("qrCodeSaveState", qrCodeSaveState);
    }
}
