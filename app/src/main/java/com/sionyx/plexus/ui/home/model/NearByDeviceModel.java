package com.sionyx.plexus.ui.home.model;

public class NearByDeviceModel {
    public String deviceName;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public NearByDeviceModel(String deviceName) {
        this.deviceName = deviceName;
    }
}
