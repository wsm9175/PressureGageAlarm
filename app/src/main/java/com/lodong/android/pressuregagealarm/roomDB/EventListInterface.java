package com.lodong.android.pressuregagealarm.roomDB;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;

import java.util.List;

public class EventListInterface {
    private EventListDao eventListDao;
    private LiveData<List<EventEntity>> eventList;

    public EventListInterface(Application application) {
        RoomDB db = RoomDB.getInstance(application);
        eventListDao = db.eventListDao();
        eventList = eventListDao.getAll();
    }

    public LiveData<List<EventEntity>> getEventList(){
        return eventList;
    }

    public void insert(EventEntity entity){
        eventListDao.insert(entity);
    }

    public void delete(EventEntity entity){
        eventListDao.delete(entity);
    }
}
