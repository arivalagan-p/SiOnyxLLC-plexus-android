package com.dome.librarynightwave.model.repository.opsinmodel;

public class DateTime {
    private int dateTime, highResolutionClock;

    public int getHighResolutionClock() {
        return highResolutionClock;
    }

    public void setHighResolutionClock(int highResolutionClock) {
        this.highResolutionClock = highResolutionClock;
    }

    public int getDateTime() {
        return dateTime;
    }

    public void setDateTime(int dateTime) {
        this.dateTime = dateTime;
    }

}
