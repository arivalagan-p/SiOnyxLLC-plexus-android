package com.dome.librarynightwave.model.repository.opsinmodel;

public class GpsPosition {
    private boolean isPowerStateOn;
    private boolean isValid;
    private double latitude;
    private double longitude;
    private char ns;
    private char ew;
    private int satelliteCount;
    private double mslAltitude;
    private String time;
    private String date;
    private String degreeFormatLatitude;
    private String degreeFormatLongitude;

    public boolean isPowerStateOn() {
        return isPowerStateOn;
    }

    public void setPowerStateOn(boolean powerStateOn) {
        isPowerStateOn = powerStateOn;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public char getNs() {
        return ns;
    }

    public void setNs(char ns) {
        this.ns = ns;
    }

    public char getEw() {
        return ew;
    }

    public void setEw(char ew) {
        this.ew = ew;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public void setSatelliteCount(int satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public double getMslAltitude() {
        return mslAltitude;
    }

    public void setMslAltitude(double mslAltitude) {
        this.mslAltitude = mslAltitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDegreeFormatLatitude() {
        return degreeFormatLatitude;
    }

    public void setDegreeFormatLatitude(String degreeFormatLatitude) {
        this.degreeFormatLatitude = degreeFormatLatitude;
    }

    public String getDegreeFormatLongitude() {
        return degreeFormatLongitude;
    }

    public void setDegreeFormatLongitude(String degreeFormatLongitude) {
        this.degreeFormatLongitude = degreeFormatLongitude;
    }
}
