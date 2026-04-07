package com.dome.librarynightwave.model.repository.pojo;


public class Packets {
    private final int sequenceNumber;
    private final int receivedLength;
    private final byte[] array;

    public Packets(int rtpSequenceNumber, int receivedLength, byte[] array) {
        this.sequenceNumber = rtpSequenceNumber;
        this.receivedLength = receivedLength;
        this.array = array;
    }

    public byte[] getArray() {
        return array;
    }

    public int getReceivedLength() {
        return receivedLength;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

}
