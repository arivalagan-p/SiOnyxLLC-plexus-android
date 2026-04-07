package com.dome.librarynightwave.model.repository.pojo;

import android.net.Network;
import android.util.Log;

import java.util.Objects;

public class NetworkChange {
    private String connectedSSID;
    private String unableToConnect;
    private Network network;
    private boolean isAvailable;
    private boolean shouldHideProgress;
    private boolean disconnectSocket;

    public String getUnableToConnect() {
        return unableToConnect;
    }

    public void setUnableToConnect(String unableToConnect) {
        this.unableToConnect = unableToConnect;
    }

    public String getConnectedSSID() {
        return connectedSSID;
    }

    public void setConnectedSSID(String connectedSSID) {
        this.connectedSSID = connectedSSID;
    }

    public boolean isShouldHideProgress() {
        return shouldHideProgress;
    }

    public void setShouldHideProgress(boolean shouldHideProgress) {
        this.shouldHideProgress = shouldHideProgress;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public boolean isDisconnectSocket() {
        return disconnectSocket;
    }

    public void setDisconnectSocket(boolean disconnectSocket) {
        this.disconnectSocket = disconnectSocket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        NetworkChange sub_cat = (NetworkChange) o;
        boolean isEqual = Objects.deepEquals(connectedSSID, sub_cat.connectedSSID) &&
                Objects.deepEquals(unableToConnect, sub_cat.unableToConnect) &&
                Objects.deepEquals(network, sub_cat.network) &&
                Objects.deepEquals(isAvailable, sub_cat.isAvailable) &&
                Objects.deepEquals(shouldHideProgress, sub_cat.shouldHideProgress);
        Log.e("TAG", "equals: "+isEqual );
        return isEqual;
    }

}
