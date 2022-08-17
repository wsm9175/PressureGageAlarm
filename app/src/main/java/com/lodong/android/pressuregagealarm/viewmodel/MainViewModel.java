package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.adapter.BTAdapter;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.roomDB.SettingListInterface;
import com.lodong.android.pressuregagealarm.view.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final String TAG = MainViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;
    private AlertDialog alertDialog;

    private BTAdapter btAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;

    private BluetoothResponseHandler handler;

    private final int BAR = 5;
    private final int PSI = 0;
    private final int KGF = 10;

    private MutableLiveData<Boolean> isloading = new MutableLiveData<>();

    private BTManager.ConnectedBluetoothThread connectedBluetoothThread;

    private MutableLiveData<Boolean> isSetting = new MutableLiveData<>();
    private long settingTime;
    private double settingDeviation;
    private String settingDeviationType;
    private List<String> settingPhoneNumber;
    private List<String> settingEmailList;


    public MainViewModel() {
    }

    public void setParent(Activity activity) {
        mRef = new WeakReference<>(activity);
    }

    public MutableLiveData<Boolean> getIsloading() {
        return isloading;
    }

    public void setHandler(BluetoothResponseHandler handler) {
        this.handler = handler;
    }

    public void settingBluetooth() {
        btManager = BTManager.getInstance();
        btManager.init(mRef.get(), getConnectSuccessListene(), handler, getBluetoothConnectListener());
        btAdapter = new BTAdapter(getBluetoothDeviceClickListener());

        if (btManager.settingBT()) {
            btManager.enabledBT(); // 블루투스 활성화
            btManager.getBtDevice();
            btManager.getMtList().observe((LifecycleOwner) mRef.get(), bluetoothDevices ->
                    btAdapter.setMList(bluetoothDevices));
        } else {
            Toast.makeText(mRef.get(), "블루투스 사용 불가능 모델입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void showDeleteDialog() {
        View dialogView = mRef.get().getLayoutInflater().inflate(R.layout.dialog_bluetooth_device_list, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mRef.get());
        builder.setView(dialogView);

        this.alertDialog = builder.create();
        alertDialog.show();

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(mRef.get(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(btAdapter);
    }

    public void changeUnit(String nowType){
        // psi, bar, kg/cm2
        nowType = nowType.trim();
        Log.d(TAG, "nowType :" + nowType);
        Log.d(TAG, "nowType length :" + nowType.length());
        String command = "^ SET UNIT ";

        if(nowType.equals("psi")){
            Log.d(TAG, "1");
            command += String.valueOf(BAR) + "\n";
        }else if(nowType.equals("bar")){
            Log.d(TAG, "2");
            command += String.valueOf(KGF) + "\n";
        }else if(nowType.equals("Kgf/Cm2")){
            Log.d(TAG, "3");
            command += String.valueOf(PSI) + "\n";
        }
        Log.d(TAG, command);
        if (!command.isEmpty()) {
            connectedBluetoothThread.write(command);
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

                MainViewModel.this.settingTime = time;
                MainViewModel.this.settingDeviation = deviation;
                MainViewModel.this.settingDeviationType = deviationType;
                MainViewModel.this.settingPhoneNumber = phoneNumberList;
                MainViewModel.this.settingEmailList = emailList;

                MainViewModel.this.isSetting.setValue(true);
            }
        });
    }

    public BluetoothDeviceClickListener getBluetoothDeviceClickListener() {
        return address -> {
            Toast.makeText(mRef.get(), "기기와 연결중입니다..", Toast.LENGTH_LONG).show();
            MainViewModel.this.isloading.setValue(true);
            this.alertDialog.dismiss();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            btManager.connectDevice(address);
        };
    }

    public ConnectSuccessListener getConnectSuccessListene() {
        return new ConnectSuccessListener() {
            @Override
            public void onSuccess(BTManager.ConnectedBluetoothThread connectedBluetoothThread) {
                Toast.makeText(mRef.get(), "기기와 연결이 성공적으로 되었습니다.", Toast.LENGTH_SHORT).show();
                MainViewModel.this.isloading.setValue(false);
                MainViewModel.this.connectedBluetoothThread = connectedBluetoothThread;
                connectedBluetoothThread.write("^ GET DATA\n");
            }

            @Override
            public void onFailed(Exception e) {
                MainViewModel.this.isloading.setValue(false);
                Toast.makeText(mRef.get(), "기기와의 통신간 오류가 발생했습니다" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }


    public BluetoothConnectListener getBluetoothConnectListener(){
        return new BluetoothConnectListener() {
            @Override
            public void isConnected() {

            }

            @Override
            public void isDisConnected(Exception e) {

            }
        };
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

    public interface BluetoothDeviceClickListener {
        public void onClick(String address);
    }

    public interface ConnectSuccessListener {
        public void onSuccess(BTManager.ConnectedBluetoothThread connectedBluetoothThread);

        public void onFailed(Exception e);
    }

    public interface BluetoothConnectListener{
        public void isConnected();
        public void isDisConnected(Exception e);
    }
}
