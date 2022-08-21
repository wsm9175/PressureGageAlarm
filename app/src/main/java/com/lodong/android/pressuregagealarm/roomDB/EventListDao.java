package com.lodong.android.pressuregagealarm.roomDB;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;

import java.util.List;

@Dao
public interface EventListDao {
    @Insert(onConflict = REPLACE)
    void insert(EventEntity entity);

    @Delete
    void delete(EventEntity entity);

    @Query("SELECT * FROM EventList")
    LiveData<List<EventEntity>> getAll();
}
