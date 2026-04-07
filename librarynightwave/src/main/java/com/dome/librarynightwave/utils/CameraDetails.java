package com.dome.librarynightwave.utils;

public class CameraDetails {

    private int rxStatus;
    private String serialNumber;

    private static String modelName;
    private static String modelNumber;
    private String manufacturer;
    private static String firmwareVersion;
    private String hardwareVersion;
    private String uuid;
    private String wifi_ssid;
    private String wifi_pwd;

    public int getRxStatus() {
        return rxStatus;
    }

    public void setRxStatus(int rxStatus) {
        this.rxStatus = rxStatus;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getWifi_ssid() {
        return wifi_ssid;
    }

    public void setWifi_ssid(String wifi_ssid) {
        this.wifi_ssid = wifi_ssid;
    }

    public String getWifi_pwd() {
        return wifi_pwd;
    }

    public void setWifi_pwd(String wifi_pwd) {
        this.wifi_pwd = wifi_pwd;
    }

    @Override
    public String toString() {
        return "CameraDetails { " +
                "modelName='" + modelName + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", hardwareVersion='" + hardwareVersion + '\'' +
                ", ssid='" + wifi_ssid + '\'' +
                ", uuid='" + uuid + '\'' +
                ", rxStatus='" + rxStatus + '\'' +
                '}';
    }

}
