package com.sionyx.plexus.ui.api.requestModel;

import com.google.gson.annotations.SerializedName;

public class RequestChangeNewPassword {
    @SerializedName("username")
    private String username;

    @SerializedName("otp")
    private String otp;

    @SerializedName("new_password")
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
