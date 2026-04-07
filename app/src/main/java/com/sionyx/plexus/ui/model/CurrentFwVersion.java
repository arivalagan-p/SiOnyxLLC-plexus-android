package com.sionyx.plexus.ui.model;

public class CurrentFwVersion {
    private String releaseVersion;
    private String fpga;
    private String riscv;
    private String wifiRtos;
    private String wifiBoot;
    private String wifiBootMode;
    private String cameraName;
    private String wifiMac;
    private String ssid;
    private String riscv_recovery;
    private String overlay;
    private String ble;

    public String getBle() {
        return ble;
    }

    public void setBle(String ble) {
        this.ble = ble;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getWifiMac() {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac) {
        this.wifiMac = wifiMac;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }


    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getFpga() {
        return fpga;
    }

    public void setFpga(String fpga) {
        this.fpga = fpga;
    }

    public String getRiscv() {
        return riscv;
    }

    public void setRiscv(String riscv) {
        this.riscv = riscv;
    }

    public String getWiFiRtos() {
        return wifiRtos;
    }

    public void setWifiRtos(String wifiRtos) {
        this.wifiRtos = wifiRtos;
    }

    public String getWifiBoot() {
        return wifiBoot;
    }

    public void setWifiBoot(String wifiBoot) {
        this.wifiBoot = wifiBoot;
    }

    public String getWifiBootMode() {
        return wifiBootMode;
    }

    public void setWifiBootMode(String wifiBootMode) {
        this.wifiBootMode = wifiBootMode;
    }

    public String getRiscvRecovery() {
        return riscv_recovery;
    }

    public void setRiscvRecovery(String riscv_recovery) {
        this.riscv_recovery = riscv_recovery;
    }

    public String getOverlay() {
        return overlay;
    }

    public void setOverlay(String overlay) {
        this.overlay = overlay;
    }
}
