package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.view.View;

import androidx.lifecycle.ViewModel;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.module.BTManager;

import java.lang.ref.WeakReference;

public class SettingViewModel extends ViewModel {
    private final String TAG = SettingViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;

    private BluetoothResponseHandler handler;

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



}
