package com.dome.librarynightwave.model.persistence.savesettings;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.time.LocalDateTime;
import java.util.List;

import io.reactivex.Single;

@Dao
public interface SaveSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSettings(SaveSettings saveSettings);

    @Query("UPDATE SaveSettings set setting_id = :setting_id, setting_name=:setting_name, setting_value=:setting_value, display_value= :display_value, is_nightwave= :is_nightwave, datetime=:datetime WHERE datetime =(SELECT MIN(datetime) FROM SaveSettings)")
    void updatePreset(int setting_id, String setting_name, byte setting_value, String display_value, boolean is_nightwave, LocalDateTime datetime);

    @Query("SELECT * FROM SaveSettings WHERE preset_name = :preset_name AND is_nightwave = :is_nightwave")
    Single<List<SaveSettings>> getSavedSettings(String preset_name, boolean is_nightwave);

    @Query("DELETE from SaveSettings WHERE preset_name=:preset_name AND is_nightwave= :is_nightwave")
    void deletePreset(String preset_name, boolean is_nightwave);

    @Query("SELECT COUNT(*) FROM SaveSettings WHERE preset_name LIKE :preset_name AND is_nightwave= :isNightwave")
    Single<Integer> isPresetAvailable(String preset_name, boolean isNightwave);

    @Query("SELECT  * FROM SaveSettings WHERE is_nightwave = :isNightwave GROUP BY preset_name, is_nightwave")
    List<SaveSettings> getSavedPreSettings(boolean isNightwave);

    @Query("SELECT COUNT(*) as count FROM savesettings WHERE is_nightwave= :isNightwave group by preset_name")
    List<Integer> getPresetCount(boolean isNightwave);
}