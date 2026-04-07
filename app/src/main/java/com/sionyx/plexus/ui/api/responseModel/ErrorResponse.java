package com.sionyx.plexus.ui.api.responseModel;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("message")
    public String message;
    private String error;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
