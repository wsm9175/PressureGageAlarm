package com.lodong.android.pressuregagealarm.roomDB;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.lodong.android.pressuregagealarm.entity.SettingEntity;

import java.util.List;

@Dao
public interface SettingListDao {
    @Insert(onConflict = REPLACE)
    void insert(SettingEntity entity);

    @Delete
    void delete(SettingEntity entity);

    @Query("SELECT * FROM settingList")
    LiveData<List<SettingEntity>> getAll();
}
