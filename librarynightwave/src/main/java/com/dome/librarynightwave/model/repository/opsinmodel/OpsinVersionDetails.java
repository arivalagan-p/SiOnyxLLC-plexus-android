package com.dome.librarynightwave.model.repository.opsinmodel;

public class OpsinVersionDetails {
    private String riscv;
    private String riscv_recovery;
    private String overlay;
    private String wifi;
    private String ble;
    private String isp;
    private String fpga;
    private byte major;
    private byte minor;
    private byte patch;

    public byte getMajor() {
        return major;
    }

    public void setMajor(byte major) {
        this.major = major;
    }

    public byte getMinor() {
        return minor;
    }

    public void setMinor(byte minor) {
        this.minor = minor;
    }

    public byte getPatch() {
        return patch;
    }

    public void setPatch(byte patch) {
        this.patch = patch;
    }

    public String getRiscv() {
        return riscv;
    }

    public void setRiscv(String riscv) {
        this.riscv = riscv;
    }

    public String getWifi() {
        return wifi;
    }

    public void setWifi(String wifi) {
        this.wifi = wifi;
    }

    public String getBle() {
        return ble;
    }

    public void setBle(String ble) {
        this.ble = ble;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getFpga() {
        return fpga;
    }

    public void setFpga(String fpga) {
        this.fpga = fpga;
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
