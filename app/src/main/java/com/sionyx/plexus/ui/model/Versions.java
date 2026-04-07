package com.sionyx.plexus.ui.model;

import com.google.gson.annotations.SerializedName;

public class Versions {
    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getFpga() {
        return fPGA;
    }

    public void setfPGA(String fPGA) {
        this.fPGA = fPGA;
    }

    public String getRiscv() {
        return rISCV;
    }

    public void setrISCV(String rISCV) {
        this.rISCV = rISCV;
    }

    public String getWiFiRtos() {
        return wIFI_RTOS;
    }

    public void setwIFI_RTOS(String wIFI_RTOS) {
        this.wIFI_RTOS = wIFI_RTOS;
    }

    public String getwIFI_BOOT() {
        return wIFI_BOOT;
    }

    public void setwIFI_BOOT(String wIFI_BOOT) {
        this.wIFI_BOOT = wIFI_BOOT;
    }

    public String getwIFI_BLE() {
        return wIFI_BLE;
    }

    public void setwIFI_BLE(String wIFI_BLE) {
        this.wIFI_BLE = wIFI_BLE;
    }

    public String getRiscvRecovery() {
        return RECOVERY;
    }

    public void setRISCV_RECOVERY(String RECOVERY) {
        this.RECOVERY = RECOVERY;
    }

    public String getOverlay() {
        return oVERLAY;
    }

    public void setOVERLAY(String oVERLAY) {
        this.oVERLAY = oVERLAY;
    }

    @SerializedName("Release")
    private String release;
    @SerializedName("FPGA")
    private String fPGA;
    @SerializedName("RISCV")
    private String rISCV;
    @SerializedName("RECOVERY")
    private String RECOVERY;
    @SerializedName("OVERLAY")
    private String oVERLAY;
    @SerializedName("WIFI_RTOS")
    private String wIFI_RTOS;
    @SerializedName("WIFI_BOOT")
    private String wIFI_BOOT;
    @SerializedName("WIFI_BLE")
    private String wIFI_BLE;
}
