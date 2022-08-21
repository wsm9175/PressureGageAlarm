package com.lodong.android.pressuregagealarm.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "EventList")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "event")
    private String event;

    @ColumnInfo(name = "occurTime")
    private long occurTime;

    public EventEntity() {}

    public EventEntity(String event, long occurTime) {
        this.event = event;
        this.occurTime = occurTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public long getOccurTime() {
        return occurTime;
    }

    public void setOccurTime(long occurTime) {
        this.occurTime = occurTime;
    }

    @Override
    public String toString() {
        return "EventEntity{" +
                "id=" + id +
                ", event='" + event + '\'' +
                ", occurTime=" + occurTime +
                '}';
    }
}
