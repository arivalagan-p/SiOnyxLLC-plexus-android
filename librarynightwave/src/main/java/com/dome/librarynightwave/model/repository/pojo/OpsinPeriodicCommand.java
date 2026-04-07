package com.dome.librarynightwave.model.repository.pojo;

import java.util.Objects;

public class OpsinPeriodicCommand {
    private boolean isGetCommand;
    private short command;
    private byte[] data;
    private boolean isResponseRequired;
    private boolean isKeepAlive;

    public boolean isGetCommand() {
        return isGetCommand;
    }

    public void setIsGetCommand(boolean getCommand) {
        isGetCommand = getCommand;
    }

    public short getCommand() {
        return command;
    }

    public void setCommand(short command) {
        this.command = command;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isResponseRequired() {
        return isResponseRequired;
    }

    public void setResponseRequired(boolean responseRequired) {
        isResponseRequired = responseRequired;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        isKeepAlive = keepAlive;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        OpsinPeriodicCommand sub_cat = (OpsinPeriodicCommand) o;
        return Objects.equals(command, sub_cat.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }
}
