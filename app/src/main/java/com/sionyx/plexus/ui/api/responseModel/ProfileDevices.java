package com.sionyx.plexus.ui.api.responseModel;

import com.google.gson.annotations.SerializedName;

public class ProfileDevices {
    @SerializedName("camera")
    public String camera;
    @SerializedName("model")
    public String model;
    @SerializedName("serialNumber")
    public String serialNumber;
    @SerializedName("classification")
    public String classification;
    @SerializedName("SKU")
    public String sKU;

    public String getCamera() {
        return camera;
    }

    public void setCamera(String camera) {
        this.camera = camera;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getsKU() {
        return sKU;
    }

    public void setsKU(String sKU) {
        this.sKU = sKU;
    }
}
