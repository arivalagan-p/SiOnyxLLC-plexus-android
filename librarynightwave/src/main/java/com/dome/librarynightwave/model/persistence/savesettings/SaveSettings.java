package com.dome.librarynightwave.model.persistence.savesettings;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.dome.librarynightwave.model.persistence.Converters;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(tableName = "SaveSettings")
@TypeConverters(Converters.class)
public class SaveSettings {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "setting_id")
    private int setting_id;

    @ColumnInfo(name = "setting_name")
    private String setting_name;

    @ColumnInfo(name = "setting_value")
    private int setting_value;

    @ColumnInfo(name = "display_value")
    private String display_value;

    @ColumnInfo(name = "is_nightwave")
    private boolean is_nightwave;

    @ColumnInfo(name = "preset_name")
    private String preset_name;

    @ColumnInfo(name = "datetime")
    private LocalDateTime datetime;

    public SaveSettings(int setting_id, String setting_name, int setting_value, String display_value,boolean is_nightwave, String preset_name, LocalDateTime datetime) {
        this.setting_id = setting_id;
        this.setting_name = setting_name;
        this.setting_value = setting_value;
        this.display_value = display_value;
        this.is_nightwave = is_nightwave;
        this.preset_name = preset_name;
        this.datetime = datetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        SaveSettings sub_cat = (SaveSettings) o;
        return Objects.equals(setting_name, sub_cat.setting_name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(setting_name);
    }

    public int getId() {
        return id;
    }

    public int getSetting_id() {
        return setting_id;
    }

    public void setSetting_id(int setting_id) {
        this.setting_id = setting_id;
    }

    public String getSetting_name() {
        return setting_name;
    }

    public void setSetting_name(String setting_name) {
        this.setting_name = setting_name;
    }

    public int getSetting_value() {
        return setting_value;
    }

    public void setSetting_value(int setting_value) {
        this.setting_value = setting_value;
    }

    public String getDisplay_value() {
        return display_value;
    }

    public void setDisplay_value(String display_value) {
        this.display_value = display_value;
    }

    public boolean isIs_nightwave() {
        return is_nightwave;
    }

    public void setIs_nightwave(boolean is_nightwave) {
        this.is_nightwave = is_nightwave;
    }

    public String getPreset_name() {
        return preset_name;
    }

    public void setPreset_name(String preset_name) {
        this.preset_name = preset_name;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }
}
