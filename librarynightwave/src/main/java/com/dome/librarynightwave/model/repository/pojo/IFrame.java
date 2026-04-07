package com.dome.librarynightwave.model.repository.pojo;

public class IFrame {
    private final long timestamp;
    private final boolean isIframe;
    private final byte[] iFrameData;
    private final byte[] spsData;
    private final byte[] ppsData;

    public IFrame(byte[] iFrameData, boolean isIframe, long timestamp, byte[] spsData, byte[] ppsData) {
        this.iFrameData = iFrameData;
        this.isIframe = isIframe;
        this.timestamp = timestamp;
        this.spsData = spsData;
        this.ppsData = ppsData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isIframe() {
        return isIframe;
    }

    public byte[] getiFrameData() {
        return iFrameData;
    }
    public byte[] getSpsData() {
        return spsData;
    }
    public byte[] getPpsData() {
        return ppsData;
    }

}
