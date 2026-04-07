package com.dome.librarynightwave.model.repository.opsinmodel;

public class CompassAngles {
    private boolean isCalibrated;
    private byte calibratedLevel;
    private short yaw;
    private short roll;

    public boolean isCalibrated() {
        return isCalibrated;
    }

    public void setCalibrated(boolean calibrated) {
        isCalibrated = calibrated;
    }

    public byte getCalibratedLevel() {
        return calibratedLevel;
    }

    public void setCalibratedLevel(byte calibratedLevel) {
        this.calibratedLevel = calibratedLevel;
    }

    public short getYaw() {
        return yaw;
    }

    public void setYaw(short yaw) {
        this.yaw = yaw;
    }

    public short getRoll() {
        return roll;
    }

    public void setRoll(short roll) {
        this.roll = roll;
    }

    public short getPitch() {
        return pitch;
    }

    public void setPitch(short pitch) {
        this.pitch = pitch;
    }

    private short pitch;


}
