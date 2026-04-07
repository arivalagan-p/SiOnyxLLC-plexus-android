package com.dome.librarynightwave.model.repository.pojo;

import com.dome.librarynightwave.model.repository.mediaCodec.RtpHeader;

public class OpsinPackets {
    private final int sequenceNumber;
    private final int receivedLength;
    private final byte[] array;

    public RtpHeader getRtpHeader() {
        return rtpHeader;
    }

    private final RtpHeader rtpHeader;

    public OpsinPackets(RtpHeader rtpHeader, int rtpSequenceNumber, int receivedLength, byte[] array) {
        this.rtpHeader = rtpHeader;
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
