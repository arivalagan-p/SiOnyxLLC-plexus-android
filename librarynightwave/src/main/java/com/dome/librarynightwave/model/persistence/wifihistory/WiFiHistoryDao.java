package com.dome.librarynightwave.model.persistence.wifihistory;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public interface WiFiHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCamera(WiFiHistory subCategory);

    @Query("UPDATE WiFiHistory SET last_connected_date_time=:timeStamp WHERE camera_ssid =:ssid")
    void updateCameraLastConnectedTime(String ssid, long timeStamp);

    @Query("UPDATE WiFiHistory SET is_wifi_connected=:state WHERE camera_ssid =:ssid")
    void updateCameraConnectionState(String ssid, int state);

    @Query("UPDATE WiFiHistory SET is_wifi_connected=:state")
    void updateAllCameraConnectionState(int state);

    @Query("UPDATE WiFiHistory SET is_wifi_connected=:state")
    void updateCameraConnectionState(int state);

    @Query("UPDATE WiFiHistory SET last_connected_date_time=:timeStamp, is_wifi_connected=:state WHERE camera_ssid =:ssid")
    void updateCameraLastConnectedTimeAndState(String ssid, long timeStamp, int state);

    @Query("UPDATE WiFiHistory SET is_wifi_connected = 0 WHERE camera_ssid !=:ssid AND is_wifi_connected == 1 ")
    void updateLastConnectedCameraState(String ssid);

    @Query("DELETE from WiFiHistory WHERE camera_ssid=:ssid")
    void delete(String ssid);

    @Query("SELECT * from WiFiHistory where camera_ssid =:ssid")
    Single<WiFiHistory> checkSsidIsExit(String ssid);

    @Query("SELECT * from WiFiHistory where camera_ssid =:ssid")
    Maybe<WiFiHistory> checkSsidDiscoverable(String ssid);

    @Query("SELECT * from WiFiHistory ORDER BY last_connected_date_time DESC LIMIT 10")
//    @Query("SELECT * from WiFiHistory ORDER BY is_wifi_connected DESC,last_connected_date_time DESC LIMIT 10")
//    @Query("SELECT * from WiFiHistory ORDER BY is_wifi_connected DESC LIMIT 10")
    Flowable<List<WiFiHistory>> getCameraList();

    @Query("SELECT camera_name from WiFiHistory WHERE camera_ssid =:ssid")
    Single<String> getCameraName(String ssid);

    @Query("SELECT * from WiFiHistory where camera_ssid =:ssid ORDER BY last_connected_date_time DESC LIMIT 10")
    boolean hasAlreadyExistSSId(String ssid);

    @Query("UPDATE WiFiHistory set camera_name = :cameraName where camera_ssid =:ssid")
    void updateCameraName(String ssid, String cameraName);

    @Query("SELECT * from WiFiHistory")
    Single<List<WiFiHistory>> getAll();
}
