package com.dome.librarynightwave.model.persistence;

import androidx.room.TypeConverter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
public class Converters {
    @TypeConverter
    public static Long fromDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    @TypeConverter
    public static LocalDateTime toDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
    }
}
