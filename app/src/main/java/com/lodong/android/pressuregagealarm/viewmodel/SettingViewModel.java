package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.roomDB.SettingListInterface;

import java.lang.ref.WeakReference;
import java.util.List;

public class SettingViewModel extends ViewModel{
    private final String TAG = SettingViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;

    private BluetoothResponseHandler handler;

    private SettingListInterface settingListInterface;

    private MutableLiveData<Boolean> isSetting = new MutableLiveData<>();
    private long settingTime;
    private double settingDeviation;
    private String settingDeviationType;
    private List<String> settingPhoneNumber;
    private List<String> settingEmailList;

    public void setParent(Activity activity) {
        mRef = new WeakReference<>(activity);
    }

    public void setHandler(BluetoothResponseHandler handler){
        this.handler = handler;
    }

    public void checkConnect(){
        btManager = BTManager.getInstance();
        if(btManager.isConnect()){
            btManager.setSettingBluetoothHandler(this.handler);
        }
    }

    public void getSettingInfo(){
        SettingListInterface settingListInterface = new SettingListInterface(mRef.get().getApplication());
        settingListInterface.getSettingList().observe((LifecycleOwner) mRef.get(), settingEntities -> {
            if(settingEntities != null){
                SettingEntity settingEntity = settingEntities.get(settingEntities.size()-1);
                long time = settingEntity.getTime();
                double deviation = settingEntity.getDeviation();
                String deviationType = settingEntity.getDeviationType();
                List<String> phoneNumberList = settingEntity.getPhoneNumberList();
                List<String> emailList = settingEntity.getEmailList();

                SettingViewModel.this.settingTime = time;
                SettingViewModel.this.settingDeviation = deviation;
                SettingViewModel.this.settingDeviationType = deviationType;
                SettingViewModel.this.settingPhoneNumber = phoneNumberList;
                SettingViewModel.this.settingEmailList = emailList;
                isSetting.setValue(true);
            }
        });
    }

    public void insertSettingInfo(long time, double deviation, List<String> phoneNumberList, List<String> emailList, String nowType){
        settingListInterface = new SettingListInterface(mRef.get().getApplication());
        SettingEntity settingEntity = new SettingEntity(time, deviation, phoneNumberList, emailList, nowType);
        settingListInterface.insert(settingEntity);
    }

    public MutableLiveData<Boolean> getIsSetting() {
        return isSetting;
    }

    public long getSettingTime() {
        return settingTime;
    }

    public void setSettingTime(long settingTime) {
        this.settingTime = settingTime;
    }

    public double getSettingDeviation() {
        return settingDeviation;
    }

    public void setSettingDeviation(double settingDeviation) {
        this.settingDeviation = settingDeviation;
    }

    public String getSettingDeviationType() {
        return settingDeviationType;
    }

    public void setSettingDeviationType(String settingDeviationType) {
        this.settingDeviationType = settingDeviationType;
    }

    public List<String> getSettingPhoneNumber() {
        return settingPhoneNumber;
    }

    public void setSettingPhoneNumber(List<String> settingPhoneNumber) {
        this.settingPhoneNumber = settingPhoneNumber;
    }

    public List<String> getSettingEmailList() {
        return settingEmailList;
    }

    public void setSettingEmailList(List<String> settingEmailList) {
        this.settingEmailList = settingEmailList;
    }
}
