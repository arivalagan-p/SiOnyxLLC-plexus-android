package com.dome.librarynightwave.model.repository.pojo;

public class RequestQueue {
    private int isValueEventAction;
    private byte[] data;

    public int getIsValueEventAction() {
        return isValueEventAction;
    }

    public void setIsValueEventAction(int isValueEventAction) {
        this.isValueEventAction = isValueEventAction;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
