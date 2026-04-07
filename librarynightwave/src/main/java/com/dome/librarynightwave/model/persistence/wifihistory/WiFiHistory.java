package com.dome.librarynightwave.model.persistence.wifihistory;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "WiFiHistory")
public class WiFiHistory {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "camera_ssid")
    private String camera_ssid;

    @ColumnInfo(name = "camera_name")
    private String camera_name;

    @ColumnInfo(name = "camera_mac_address")
    private String camera_mac_address;

    @ColumnInfo(name = "camera_fw_version")
    private String camera_fw_version;

    @ColumnInfo(name = "camera_password")
    private String camera_password;

    @ColumnInfo(name = "last_connected_date_time")
    private long last_connected_date_time = 0;

    @ColumnInfo(name = "is_wifi_connected")
    private int is_wifi_connected = -1;

    @Ignore
    public WiFiHistory() {

    }

    public WiFiHistory(String camera_ssid, String camera_name, String camera_mac_address, String camera_fw_version,
                       String camera_password, long last_connected_date_time, int is_wifi_connected) {
        this.camera_ssid = camera_ssid;
        this.camera_name = camera_name;
        this.camera_mac_address = camera_mac_address;
        this.camera_fw_version = camera_fw_version;
        this.camera_password = camera_password;
        this.last_connected_date_time = last_connected_date_time;
        this.is_wifi_connected = is_wifi_connected;
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

    public String getCamera_mac_address() {
        return camera_mac_address;
    }

    public void setCamera_mac_address(String camera_mac_address) {
        this.camera_mac_address = camera_mac_address;
    }

    public String getCamera_fw_version() {
        return camera_fw_version;
    }

    public void setCamera_fw_version(String camera_fw_version) {
        this.camera_fw_version = camera_fw_version;
    }

    public String getCamera_password() {
        return camera_password;
    }

    public void setCamera_password(String camera_password) {
        this.camera_password = camera_password;
    }

    public long getLast_connected_date_time() {
        return last_connected_date_time;
    }

    public void setLast_connected_date_time(long last_connected_date_time) {
        this.last_connected_date_time = last_connected_date_time;
    }

    public int getIs_wifi_connected() {
        return is_wifi_connected;
    }

    public void setIs_wifi_connected(int is_wifi_connected) {
        this.is_wifi_connected = is_wifi_connected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        WiFiHistory sub_cat = (WiFiHistory) o;
        return Objects.equals(camera_ssid, sub_cat.camera_ssid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera_ssid);
    }
}
