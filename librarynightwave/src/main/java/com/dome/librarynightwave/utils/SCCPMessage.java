package com.dome.librarynightwave.utils;

import static com.dome.librarynightwave.utils.SCCPConstants.GET_SET_PACKET_HEADER_LENGTH;

public class SCCPMessage {
    static SCCPMessage sccpMessage;

    public SCCPMessage() {
    }

    public static SCCPMessage getInstance() {
        if (sccpMessage == null) {
            sccpMessage = new SCCPMessage();
        }
        return sccpMessage;
    }

    private byte msg_type;
    private byte valueEventActionIds;
    private short block_num = 0;
    private short data_size = 0;
    private short sequence_num;
    private byte priority;
    private byte[] reserved = new byte[]{0x0, 0x0, 0x0};
    private byte[] data;


    public byte getMsg_type() {
        return msg_type;
    }

    public void setMsg_type(byte msg_type) {
        this.msg_type = msg_type;
    }

    public byte getValueEventActionIds() {
        return valueEventActionIds;
    }

    public void setValueEventActionIds(byte valueEventActionIds) {
        this.valueEventActionIds = valueEventActionIds;
    }

    public short getBlock_num() {
        return block_num;
    }

    public void setBlock_num(short block_num) {
        this.block_num = block_num;
    }

    public short getData_size() {
        return data_size;
    }

    public void setData_size(short data_size) {
        this.data_size = data_size;
    }

    public short getSequence_num() {
        return sequence_num;
    }

    public void setSequence_num(short sequence_num) {
        this.sequence_num = sequence_num;
    }

    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    public byte[] getReserved() {
        return reserved;
    }

    public void setReserved(byte[] reserved) {
        this.reserved = reserved;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getTotalLength() {
        if (data == null)
            return GET_SET_PACKET_HEADER_LENGTH;
        else
            return GET_SET_PACKET_HEADER_LENGTH + data.length;
    }
}
