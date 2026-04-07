package com.dome.librarynightwave.model.repository.mediaCodec;

/*
 *In this implementation,
 * version is the RTP protocol version, padding indicates if the packet has padding bytes, extension indicates if the packet has header extensions,
 * csrcCount is the number of contributing source identifiers in the packet, marker indicates if the packet has special meaning,
 * payloadType is the type of payload data in the packet, sequenceNumber is the sequence number of the packet, timestamp is the timestamp of the packet,
 * and ssrc is the synchronization source identifier of the packet.
 * The fromBytes method parses an RTP packet and extracts the header fields.
 * It creates a new RtpHeader object and sets the fields based on the byte values in the packet.
 * The getHeaderLength method returns the length of the RTP header in bytes.
 */
public class RtpHeader {
    private static final int RTP_HEADER_SIZE = 12;
    private int version;
    private boolean padding;
    private boolean extension;
    private int csrcCount;
    private boolean marker;
    private int payloadType;
    private int sequenceNumber;
    private long timestamp;
    private int ssrc;
    private int NALCount;

    private RtpHeader() {
    }

    public int getVersion() {
        return version;
    }

    public boolean hasPadding() {
        return padding;
    }

    public boolean hasExtension() {
        return extension;
    }

    public int getCsrcCount() {
        return csrcCount;
    }

    public boolean isMarker() {
        return marker;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getSsrc() {
        return ssrc;
    }

    public int getHeaderLength() {
        return RTP_HEADER_SIZE + NALCount * csrcCount;
    }

    public static RtpHeader fromBytes(byte[] bytes) {
        RtpHeader header = new RtpHeader();

        int b1 = bytes[0];
        int b2 = bytes[1];
        int b3 = bytes[2];
        int b4 = bytes[3];

        header.version = (b1 >> 6) & 0x03;
        header.padding = ((b1 >> 5) & 0x01) == 1;
        header.extension = ((b1 >> 4) & 0x01) == 1;
        header.csrcCount = b1 & 0x0f;
        header.marker = ((b2 >> 7) & 0x01) == 1;
        header.payloadType = b2 & 0x7f;

        header.sequenceNumber = ((b3 & 0xff) << 8) | (b4 & 0xff);

        header.timestamp = ((long) (bytes[4] & 0xff) << 24) |
                ((long) (bytes[5] & 0xff) << 16) |
                ((long) (bytes[6] & 0xff) << 8) |
                ((long) (bytes[7] & 0xff));

        header.ssrc = ((bytes[8] & 0xff) << 24) |
                ((bytes[9] & 0xff) << 16) |
                ((bytes[10] & 0xff) << 8) |
                (bytes[11] & 0xff);

        return header;
    }

    public void setNALCount(int count) {
        NALCount = count;
    }

    public byte[] getRtpPayloadData(byte[] rtpPacketData) {
        int headerLength = 12;
        int payloadLength = rtpPacketData.length - headerLength;
        byte[] rtpPayloadData = new byte[payloadLength];
        System.arraycopy(rtpPacketData, headerLength, rtpPayloadData, 0, payloadLength);
        return rtpPayloadData;
    }
}

