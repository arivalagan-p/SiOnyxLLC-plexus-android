package com.dome.librarynightwave.model.repository.opsinmodel;

public class LastSentCommand {
    private boolean isSetCommand;
    private short command;
    private boolean isResponseRequired;
    private byte[] data;
    private boolean isKeepAlive;


    public boolean isSetCommand() {
        return isSetCommand;
    }

    public void setIsSetCommand(boolean setCommand) {
        isSetCommand = setCommand;
    }

    public short getCommand() {
        return command;
    }

    public void setCommand(short command) {
        this.command = command;
    }

    public boolean isResponseRequired() {
        return isResponseRequired;
    }

    public void setResponseRequired(boolean responseRequired) {
        isResponseRequired = responseRequired;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        isKeepAlive = keepAlive;
    }


}
