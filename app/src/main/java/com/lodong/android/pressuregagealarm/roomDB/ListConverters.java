package com.lodong.android.pressuregagealarm.roomDB;

import androidx.room.ProvidedTypeConverter;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

public class ListConverters implements Serializable {
    @TypeConverter
    public static List<String> fromString(String value){
        Type listType = new TypeToken<List<String>>(){}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<String> mList){
        return new Gson().toJson(mList);
    }
}
