package com.dome.librarynightwave.model.persistence;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.dome.librarynightwave.model.persistence.savesettings.SaveSettings;
import com.dome.librarynightwave.model.persistence.savesettings.SaveSettingsDao;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.NightwaveDigitalWiFiHistoryDao;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistory;
import com.dome.librarynightwave.model.persistence.wifihistory.WiFiHistoryDao;


@Database(entities = {WiFiHistory.class, SaveSettings.class, NightwaveDigitalWiFiHistory.class}, version = 4,exportSchema = false)

@TypeConverters({Converters.class})
public abstract class DomeRoomDatabase extends RoomDatabase {
    private static volatile DomeRoomDatabase INSTANCE;
    private static final Callback sCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
        }

        @Override
        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
            super.onDestructiveMigration(db);
        }
    };

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new table
            database.execSQL("CREATE TABLE IF NOT EXISTS SaveSettings " +
                    "(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "setting_id INTEGER NOT NULL, " +
                    "setting_name TEXT, " +
                    "setting_value INTEGER NOT NULL, " +
                    "display_value TEXT," +
                    "is_nightwave INTEGER NOT NULL," +
                    "preset_name TEXT, " +
                    "datetime INTEGER" +
                    ")");


            // Create the new table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS WiFiHistoryTmp " +
                            "(" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "camera_ssid TEXT, " +
                            "camera_name TEXT, " +
                            "camera_mac_address TEXT, " +
                            "camera_fw_version TEXT," +
                            "camera_password TEXT," +
                            "last_connected_date_time INTEGER NOT NULL, " +
                            "is_wifi_connected INTEGER NOT NULL" +
                            ")");

            // Copy the data
            database.execSQL(
                    "INSERT INTO WiFiHistoryTmp (id,camera_ssid, camera_name, camera_mac_address ,camera_fw_version, camera_password, last_connected_date_time, is_wifi_connected) " +
                            "SELECT id,camera_ssid, camera_name, camera_mac_address ,camera_fw_version, camera_password, last_connected_date_time, is_wifi_connected FROM WiFiHistory");

            // Remove the old table
            database.execSQL("DROP TABLE WiFiHistory");

            // Change the table name to the correct one
            database.execSQL("ALTER TABLE WiFiHistoryTmp RENAME TO WiFiHistory");

        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Create the new table
            db.execSQL("CREATE TABLE IF NOT EXISTS NightwaveDigitalWiFiHistory (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "camera_ssid TEXT, " +
                    "camera_name TEXT, " +
                    "camera_fw_version TEXT, " +
                    "last_popup_displayed_date INTEGER NOT NULL," +
                    "camera_password TEXT," +
                    "is_auto_connected INTEGER NOT NULL" +
                    ")");
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            database.execSQL("CREATE TABLE IF NOT EXISTS NightwaveDigitalWiFiHistoryTmp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "camera_ssid TEXT, " +
                    "camera_name TEXT, " +
                    "camera_fw_version TEXT, " +
                    "last_popup_displayed_date INTEGER NOT NULL DEFAULT 0, " +
                    "camera_password TEXT DEFAULT '', " +
                    "is_auto_connected INTEGER NOT NULL DEFAULT 0" +
                    ")");

            database.execSQL(
                    "INSERT INTO NightwaveDigitalWiFiHistoryTmp " +
                            "(id, camera_ssid, camera_name, camera_fw_version, last_popup_displayed_date, camera_password, is_auto_connected) " +
                            "SELECT id, camera_ssid, camera_name, camera_fw_version, 0, '', 0 " +
                            "FROM NightwaveDigitalWiFiHistory");

            database.execSQL("DROP TABLE NightwaveDigitalWiFiHistory");

            database.execSQL("ALTER TABLE NightwaveDigitalWiFiHistoryTmp RENAME TO NightwaveDigitalWiFiHistory");
        }
    };


    public static DomeRoomDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DomeRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), DomeRoomDatabase.class, "DB_SIONYX_NIGHT_WAVE")
                            .allowMainThreadQueries()
                            .addMigrations(DomeRoomDatabase.MIGRATION_1_2,DomeRoomDatabase.MIGRATION_2_3,DomeRoomDatabase.MIGRATION_3_4)
//                            .addTypeConverter(Converters.class)
                            .addCallback(sCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract WiFiHistoryDao cameraInfoDao();

    public abstract SaveSettingsDao saveSettingsDao();

    public abstract NightwaveDigitalWiFiHistoryDao nwdCameraInfoDao();

}
