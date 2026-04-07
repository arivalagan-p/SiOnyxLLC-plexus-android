package com.dome.librarynightwave.model.repository.opsinmodel;

public class BatteryInfo {
    private byte id, state;
    private short adc, voltage, temperature;

    public byte getId() {
        return id;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public short getAdc() {
        return adc;
    }

    public void setAdc(short adc) {
        this.adc = adc;
    }

    public short getVoltage() {
        return voltage;
    }

    public void setVoltage(short voltage) {
        this.voltage = voltage;
    }

    public short getTemperature() {
        return temperature;
    }

    public void setTemperature(short temperature) {
        this.temperature = temperature;
    }
}
