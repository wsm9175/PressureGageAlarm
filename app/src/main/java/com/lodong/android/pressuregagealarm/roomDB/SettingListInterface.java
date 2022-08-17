package com.lodong.android.pressuregagealarm.roomDB;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.lodong.android.pressuregagealarm.entity.SettingEntity;

import java.util.List;

public class SettingListInterface {
    private SettingListDao settingListDao;
    private LiveData<List<SettingEntity>> settingList;

    public SettingListInterface(Application application) {
        RoomDB db = RoomDB.getInstance(application);
        settingListDao = db.settingListDao();
        settingList = settingListDao.getAll();
    }

    public LiveData<List<SettingEntity>> getSettingList(){
        return settingList;
    }

    public void insert(SettingEntity entity){
        settingListDao.insert(entity);
    }

    public void delete(SettingEntity entity){
        settingListDao.delete(entity);
    }

}
