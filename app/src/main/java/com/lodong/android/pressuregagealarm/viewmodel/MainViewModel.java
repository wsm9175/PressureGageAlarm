package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.adapter.BTAdapter;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.view.MainActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainViewModel extends ViewModel {
    private final String TAG = MainViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;
    private AlertDialog alertDialog;

    private BTAdapter btAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;

    private MainActivity.BluetoothResponseHandler handler;

    private final int BAR = 5;
    private final int PSI = 0;
    private final int KGF = 10;

    private MutableLiveData<Boolean> isloading = new MutableLiveData<>();

    private BTManager.ConnectedBluetoothThread connectedBluetoothThread;

    public MainViewModel() {
    }

    public void setParent(Activity activity) {
        mRef = new WeakReference<>(activity);
    }

    public MutableLiveData<Boolean> getIsloading() {
        return isloading;
    }

    public void setHandler(MainActivity.BluetoothResponseHandler handler) {
        this.handler = handler;
    }

    public void settingBluetooth() {
        btManager = new BTManager(mRef.get(), getConnectSuccessListene(), handler);
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
            public void onFailed() {
                MainViewModel.this.isloading.setValue(false);
            }
        };
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

    public interface BluetoothDeviceClickListener {
        public void onClick(String address);
    }

    public interface ConnectSuccessListener {
        public void onSuccess(BTManager.ConnectedBluetoothThread connectedBluetoothThread);

        public void onFailed();
    }
}
