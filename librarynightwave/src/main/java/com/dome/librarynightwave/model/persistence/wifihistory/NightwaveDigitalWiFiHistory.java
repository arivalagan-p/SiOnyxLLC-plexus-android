package com.dome.librarynightwave.model.persistence.wifihistory;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "NightwaveDigitalWiFiHistory")
public class NightwaveDigitalWiFiHistory {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "camera_ssid")
    private String camera_ssid;

    @ColumnInfo(name = "camera_name")
    private String camera_name;

    @ColumnInfo(name = "camera_fw_version")
    private String camera_fw_version;

    @ColumnInfo(name = "last_popup_displayed_date")
    private long last_popup_displayed_date = 0;

    @ColumnInfo(name = "camera_password")
    private String camera_password;

    @ColumnInfo(name = "is_auto_connected")
    private int is_auto_connected = -1;


    @Ignore
    public NightwaveDigitalWiFiHistory() {

    }
    public NightwaveDigitalWiFiHistory(String camera_ssid, String camera_name, String camera_fw_version, long last_popup_displayed_date, String camera_password, int is_auto_connected) {
        this.camera_ssid = camera_ssid;
        this.camera_name = camera_name;
        this.camera_fw_version = camera_fw_version;
        this.last_popup_displayed_date = last_popup_displayed_date;
        this.camera_password = camera_password;
        this.is_auto_connected = is_auto_connected;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCamera_ssid() {
        return camera_ssid;
    }

    public void setCamera_ssid(String camera_ssid) {
        this.camera_ssid = camera_ssid;
    }

    public String getCamera_name() {
        return camera_name;
    }

    public void setCamera_name(String camera_name) {
        this.camera_name = camera_name;
    }

    public String getCamera_fw_version() {
        return camera_fw_version;
    }

    public void setCamera_fw_version(String camera_fw_version) {
        this.camera_fw_version = camera_fw_version;
    }

    public long getLast_popup_displayed_date() {
        return last_popup_displayed_date;
    }

    public void setLast_popup_displayed_date(long last_popup_displayed_date) {
        this.last_popup_displayed_date = last_popup_displayed_date;
    }

    public String getCamera_password() {
        return camera_password;
    }

    public void setCamera_password(String camera_password) {
        this.camera_password = camera_password;
    }

    public int getIs_auto_connected() {
        return is_auto_connected;
    }

    public void setIsAutoConnected(int is_auto_connected) {
        this.is_auto_connected = is_auto_connected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        NightwaveDigitalWiFiHistory sub_cat = (NightwaveDigitalWiFiHistory) o;
        return Objects.equals(camera_ssid, sub_cat.camera_ssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera_ssid);
    }
}

