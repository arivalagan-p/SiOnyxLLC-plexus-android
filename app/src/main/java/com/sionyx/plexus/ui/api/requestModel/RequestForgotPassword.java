package com.sionyx.plexus.ui.api.requestModel;

import com.google.gson.annotations.SerializedName;

public class RequestForgotPassword {
    @SerializedName("username")
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
