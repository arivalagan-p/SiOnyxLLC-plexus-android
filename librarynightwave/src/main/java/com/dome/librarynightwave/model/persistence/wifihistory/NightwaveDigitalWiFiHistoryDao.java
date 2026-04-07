package com.dome.librarynightwave.model.persistence.wifihistory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import io.reactivex.Single;

@Dao
public interface NightwaveDigitalWiFiHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCamera(NightwaveDigitalWiFiHistory subCategory);

    @Query("SELECT * from NightwaveDigitalWiFiHistory where camera_ssid =:ssid")
    Single<NightwaveDigitalWiFiHistory> checkSsidIsExit(String ssid);

    @Query("UPDATE NightwaveDigitalWiFiHistory SET last_popup_displayed_date =:lastDisplayDate WHERE camera_ssid =:ssid")
    void updateLastFWDisplayDate(String ssid, long lastDisplayDate);

    @Query("DELETE from NightwaveDigitalWiFiHistory WHERE camera_ssid=:ssid")
    void delete(String ssid);

    @Query("UPDATE NightwaveDigitalWiFiHistory SET camera_password=:password WHERE camera_ssid =:ssid")
    void updateCameraPassword(String ssid, String password);

    @Query("UPDATE NightwaveDigitalWiFiHistory SET camera_password=:password, is_auto_connected=:state WHERE camera_ssid =:ssid")
    void updateCameraConnectionStateAndPassword(String ssid, String password, int state);

    @Query("UPDATE NightwaveDigitalWiFiHistory SET is_auto_connected=:state WHERE camera_ssid =:ssid")
    void updateCameraAutoConnect(String ssid, int state);

    @Query("SELECT camera_password from NightwaveDigitalWiFiHistory WHERE camera_ssid =:ssid")
    Single<String> getCameraPassword(String ssid);

    @Query("UPDATE NightwaveDigitalWiFiHistory SET camera_fw_version=:firmware WHERE camera_ssid =:ssid")
    void updateCameraFirmwareVersion(String ssid, String firmware);
}
