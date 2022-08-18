package com.lodong.android.pressuregagealarm.roomDB;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.lodong.android.pressuregagealarm.entity.SettingEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities =  {SettingEntity.class}, version = 4, exportSchema = false)
@TypeConverters(com.lodong.android.pressuregagealarm.roomDB.ListConverters.class)
public abstract class SettingListRoomDB extends RoomDatabase {
    private final String TAG = SettingListRoomDB.class.getSimpleName();
    private static String DATABASE_NAME = "settinglist";
    private static SettingListRoomDB instance;

    public static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExcutor
            = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public synchronized static SettingListRoomDB getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(), SettingListRoomDB.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract SettingListDao settingListDao();
}
