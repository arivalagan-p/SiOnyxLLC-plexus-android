package com.dome.librarynightwave.model.repository.pojo;

public class RTPStats {
    private int goodFrames,
            lastReceivedFrameSize,
            fps,
            receivedUdpPackets,
            skippedPackets,
            jpegBadStart,
            jpegBadEnd,
            jpegBadOffset;

    public int getGoodFrames() {
        return goodFrames;
    }

    public void setGoodFrames(int goodFrames) {
        this.goodFrames = goodFrames;
    }

    public int getLastReceivedFrameSize() {
        return lastReceivedFrameSize;
    }

    public void setLastReceivedFrameSize(int lastReceivedFrameSize) {
        this.lastReceivedFrameSize = lastReceivedFrameSize;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getReceivedUdpPackets() {
        return receivedUdpPackets;
    }

    public void setReceivedUdpPackets(int receivedUdpPackets) {
        this.receivedUdpPackets = receivedUdpPackets;
    }

    public int getSkippedPackets() {
        return skippedPackets;
    }

    public void setSkippedPackets(int skippedPackets) {
        this.skippedPackets = skippedPackets;
    }

    public int getJpegBadStart() {
        return jpegBadStart;
    }

    public void setJpegBadStart(int jpegBadStart) {
        this.jpegBadStart = jpegBadStart;
    }

    public int getJpegBadEnd() {
        return jpegBadEnd;
    }

    public void setJpegBadEnd(int jpegBadEnd) {
        this.jpegBadEnd = jpegBadEnd;
    }

    public int getJpegBadOffset() {
        return jpegBadOffset;
    }

    public void setJpegBadOffset(int jpegBadOffset) {
        this.jpegBadOffset = jpegBadOffset;
    }
}
