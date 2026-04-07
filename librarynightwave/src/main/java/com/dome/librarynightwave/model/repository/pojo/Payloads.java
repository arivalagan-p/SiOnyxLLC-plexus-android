package com.dome.librarynightwave.model.repository.pojo;

public class Payloads {
    private int receivedLength = 0;
    private byte[] payload;

    public int getReceivedLength() {
        return receivedLength;
    }

    public void setReceivedLength(int receivedLength) {
        this.receivedLength = receivedLength;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }
}
