package com.lodong.android.pressuregagealarm.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.lodong.android.pressuregagealarm.roomDB.ListConverters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "settingList")
public class SettingEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "time")
    private long time;//0.5시간, 1시간, 4시간, 8시간, 12시간, 24시간
    @ColumnInfo(name = "deviation")
    private double deviation;
    @ColumnInfo(name = "phoneNumberList")
    private List<String> phoneNumberList;
    @ColumnInfo(name = "emailList")
    private List<String> emailList;

    public SettingEntity(long time, double deviation, List<String> phoneNumberList, List<String> emailList) {
        this.time = time;
        this.deviation = deviation;
        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getDeviation() {
        return deviation;
    }

    public void setDeviation(double deviation) {
        this.deviation = deviation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<String> getPhoneNumberList() {
        return phoneNumberList;
    }

    public void setPhoneNumberList(List<String> phoneNumberList) {
        this.phoneNumberList = phoneNumberList;
    }

    public List<String> getEmailList() {
        return emailList;
    }

    public void setEmailList(List<String> emailList) {
        this.emailList = emailList;
    }

    @Override
    public String toString() {
        return "SettingEntity{" +
                "id=" + id +
                ", time=" + time +
                ", deviation=" + deviation +
                ", phoneNumberList=" + phoneNumberList +
                ", emailList=" + emailList +
                '}';
    }
}
