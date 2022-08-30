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
    private long time=-99;//0.5시간, 1시간, 4시간, 8시간, 12시간, 24시간
    @ColumnInfo(name = "deviation")
    private double deviation=-99;
    @ColumnInfo(name="deviationType")
    private String deviationType;
    @ColumnInfo(name = "phoneNumberList")
    private List<String> phoneNumberList;
    @ColumnInfo(name = "emailList")
    private List<String> emailList;
    @ColumnInfo(name = "messageMent")
    private String messageMent;

    public SettingEntity(long time, double deviation, List<String> phoneNumberList, List<String> emailList, String nowType, String messageMent) {
        this.time = time;
        this.deviation = deviation;
        this.phoneNumberList = phoneNumberList;
        this.emailList = emailList;
        this.deviationType = nowType;
        this.messageMent = messageMent;
    }

    public SettingEntity() {

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

    public String getDeviationType() {
        return deviationType;
    }

    public void setDeviationType(String deviationType) {
        this.deviationType = deviationType;
    }

    public String getMessageMent() {
        return messageMent;
    }

    public void setMessageMent(String messageMent) {
        this.messageMent = messageMent;
    }

    @Override
    public String toString() {
        return "SettingEntity{" +
                "id=" + id +
                ", time=" + time +
                ", deviation=" + deviation +
                ", deviationType='" + deviationType + '\'' +
                ", phoneNumberList=" + phoneNumberList +
                ", emailList=" + emailList +
                ", messageMent='" + messageMent + '\'' +
                '}';
    }
}
