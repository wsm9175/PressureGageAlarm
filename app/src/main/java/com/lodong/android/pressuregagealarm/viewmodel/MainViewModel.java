package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.adapter.BTAdapter;
import com.lodong.android.pressuregagealarm.entity.BroadCastAction;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.roomDB.SettingListInterface;
import com.lodong.android.pressuregagealarm.service.RecordService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final String TAG = MainViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;
    private AlertDialog alertDialog;

    private MutableLiveData<Boolean> isBluetoothDeviceConnect = new MutableLiveData<>();
    private BTAdapter btAdapter;

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

    private final double[] VALUE_PSI = {1.422339, 4.267018, 7.111696};
    private final double[] VALUE_BAR = {0.098067, 0.294199, 0.490332};
    private final double[] VALUE_KGF = {0.1, 0.3, 0.5};

    //녹화 관련 변수
    private MutableLiveData<Boolean> isNowRecord = new MutableLiveData<>();
    private MutableLiveData<Long> progressTimeML = new MutableLiveData<>();
    private MutableLiveData<String> startTimeML = new MutableLiveData<>();
    private MutableLiveData<String> endTimeML = new MutableLiveData<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "breadcast receiver "+action);
            if (BroadCastAction.PROGRESS_TIME.equals(action)) {
                //남은 시간 계산
                long nowProgressTime = intent.getLongExtra(BroadCastAction.PROGRESS_TIME, 0);
                long lastTime = MainViewModel.this.settingTime - nowProgressTime;
                progressTimeML.setValue(lastTime);
            }
            if (BroadCastAction.START_TIME.equals(action)) {
                startTimeML.setValue(intent.getStringExtra(BroadCastAction.START_TIME));
            }
            if (BroadCastAction.END_TIME.equals(action)) {
                endTimeML.setValue(intent.getStringExtra(BroadCastAction.END_TIME));
            }
            if(BroadCastAction.END_RECORD.equals(action)){
                Log.d(TAG, BroadCastAction.END_RECORD);
                endRecord();
            }
        }
    };

    private IntentFilter intentFilter;

    private Intent recordServiceIntent;


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

    public void changeUnit(String nowType) {
        // psi, bar, kg/cm2
        nowType = nowType.trim();
        Log.d(TAG, "nowType :" + nowType);
        Log.d(TAG, "nowType length :" + nowType.length());
        String command = "^ SET UNIT ";
        int position = 0;
        if (nowType.equals("psi")) {
            Log.d(TAG, "1");
            command += String.valueOf(BAR) + "\n";

            for (int i = 0; i < VALUE_PSI.length; i++) {
                Log.d(TAG, "Double.compare(this.settingDeviation, VALUE_PSI[i])" + Double.compare(this.settingDeviation, VALUE_PSI[i]));
                if (Double.compare(this.settingDeviation, VALUE_PSI[i]) == 0) {
                    position = i;
                    break;
                }
            }

            this.settingDeviation = VALUE_BAR[position];
            this.settingDeviationType = "bar";
        } else if (nowType.equals("bar")) {
            Log.d(TAG, "2");
            command += String.valueOf(KGF) + "\n";
            for (int i = 0; i < VALUE_PSI.length; i++) {
                if (Double.compare(this.settingDeviation, VALUE_BAR[i]) == 0) {
                    position = i;
                }
            }
            this.settingDeviation = VALUE_KGF[position];
            this.settingDeviationType = "Kgf/Cm2";
        } else if (nowType.equals("Kgf/Cm2")) {
            Log.d(TAG, "3");
            command += String.valueOf(PSI) + "\n";

            for (int i = 0; i < VALUE_PSI.length; i++) {
                if (Double.compare(this.settingDeviation, VALUE_KGF[i]) == 0) {
                    position = i;
                }
            }

            this.settingDeviation = VALUE_PSI[position];
            this.settingDeviationType = "psi";
        }
        Log.d(TAG, command);
        if (!command.isEmpty()) {
            connectedBluetoothThread.write(command);
        }
    }

    public void changeDeviationType(String nowType) {
        Log.d(TAG, "settingDeviationType : " + settingDeviationType);
        Log.d(TAG, "nowType : " + nowType);
        if (this.settingDeviationType.equals("psi")) {
            int position = 0;
            for (int i = 0; i < 3; i++) {
                Log.d(TAG, "Double.compare(VALUE_PSI[i], this.settingDeviation : " + Double.compare(VALUE_PSI[i], this.settingDeviation));
                if (Double.compare(VALUE_PSI[i], this.settingDeviation) == 0) {
                    position = i;
                    break;
                }
            }

            if (nowType.equals("psi")) {
                /*this.nowDeviation = VALUE_BAR[position];*/
            } else if (nowType.equals("bar")) {
                this.settingDeviation = VALUE_BAR[position];
                this.settingDeviationType = "bar";
            } else if (nowType.equals("Kgf/Cm2")) {
                this.settingDeviation = VALUE_KGF[position];
                this.settingDeviationType = "Kgf/Cm2";
            }
        } else if (this.settingDeviationType.equals("bar")) {
            int position = 0;
            for (int i = 0; i < 3; i++) {
                if (Double.compare(VALUE_BAR[i], this.settingDeviation) == 0) {
                    position = i;
                    break;
                }
            }
            if (nowType.equals("psi")) {
                this.settingDeviation = VALUE_PSI[position];
                this.settingDeviationType = "psi";
            } else if (nowType.equals("bar")) {
                /*this.nowDeviation = VALUE_BAR[position];*/
            } else if (nowType.equals("Kgf/Cm2")) {
                this.settingDeviation = VALUE_KGF[position];
                this.settingDeviationType = "Kgf/Cm2";
            }
        } else if (this.settingDeviationType.equals("Kgf/Cm2")) {
            int position = 0;
            for (int i = 0; i < 3; i++) {
                if (Double.compare(VALUE_KGF[i], this.settingDeviation) == 0) {
                    position = i;
                    break;
                }
            }
            if (nowType.equals("psi")) {
                this.settingDeviation = VALUE_PSI[position];
                this.settingDeviationType = "psi";
            } else if (nowType.equals("bar")) {
                this.settingDeviation = VALUE_BAR[position];
                this.settingDeviationType = "bar";
            } else if (nowType.equals("Kgf/Cm2")) {
                /*this.nowDeviation = VALUE_BAR[position];*/
            }
        }

    }

    public void getSettingInfo() {
        SettingListInterface settingListInterface = new SettingListInterface(mRef.get().getApplication());
        settingListInterface.getSettingList().observe((LifecycleOwner) mRef.get(), settingEntities -> {
            if (settingEntities != null && settingEntities.size() != 0) {
                SettingEntity settingEntity = settingEntities.get(settingEntities.size() - 1);
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

    public void showRecordStartDialog(double nowValue) {
        View dialogView = mRef.get().getLayoutInflater().inflate(R.layout.dialog_record_start, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mRef.get());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);

        inputButton.setOnClickListener(view -> {
            //record start;
            alertDialog.dismiss();
            recordStart(nowValue);
        });
        cancelButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });

    }

    private void recordStart(double nowValue) {
        //init
        recordServiceIntent = new Intent(mRef.get(), RecordService.class);
        recordServiceIntent.putExtra("nowValue", nowValue);
        recordServiceIntent.putExtra("settingTime", this.settingTime);
        recordServiceIntent.putExtra("settingDeviation", this.settingDeviation);
        recordServiceIntent.putStringArrayListExtra("settingPhoneNumber", (ArrayList<String>) this.settingPhoneNumber);
        recordServiceIntent.putStringArrayListExtra("settingEmailList", (ArrayList<String>) this.settingEmailList);
        isNowRecord.setValue(true);
        mRef.get().startService(recordServiceIntent);
        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadCastAction.PROGRESS_TIME);
        intentFilter.addAction(BroadCastAction.START_TIME);
        intentFilter.addAction(BroadCastAction.END_TIME);
        intentFilter.addAction(BroadCastAction.END_RECORD);
        mRef.get().registerReceiver(this.receiver, intentFilter);

        long nowTime = System.currentTimeMillis();
        String recordStartTime = getTime(nowTime);
        String recordEnd = getTime(nowTime + this.settingTime);
        this.startTimeML.setValue(recordStartTime);
        this.endTimeML.setValue(recordEnd);
    }

    private void endRecord() {
        this.isNowRecord.postValue(false);
        this.startTimeML.postValue("00:00:00");
        this.endTimeML.postValue("00:00:00");
        if (recordServiceIntent != null) {
            mRef.get().stopService(recordServiceIntent);
            recordServiceIntent = null;
            intentFilter = null;
        }
    }

    public void showRecordEndDialog() {
        View dialogView = mRef.get().getLayoutInflater().inflate(R.layout.dialog_record_end, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mRef.get());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        Button inputButton = dialogView.findViewById(R.id.btn_ok);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);

        inputButton.setOnClickListener(view -> {
            //record end
            alertDialog.dismiss();
            endRecord();
        });
        cancelButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    private String getTime(long time) {
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd k:mm:ss");

        mDate = new Date(time);
        return mFormat.format(mDate);
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
                MainViewModel.this.isloading.postValue(false);
                MainViewModel.this.connectedBluetoothThread = connectedBluetoothThread;
                isBluetoothDeviceConnect.postValue(true);
                connectedBluetoothThread.write("^ GET DATA\n");
            }

            @Override
            public void onFailed(Exception e) {
                MainViewModel.this.isloading.postValue(false);
                isBluetoothDeviceConnect.postValue(false);
            }
        };
    }


    public BluetoothConnectListener getBluetoothConnectListener() {
        return new BluetoothConnectListener() {
            @Override
            public void isConnected() {

            }

            @Override
            public void isDisConnected(Exception e) {
                isBluetoothDeviceConnect.postValue(false);
            }
        };
    }


    public MutableLiveData<Boolean> getIsBluetoothDeviceConnect() {
        return isBluetoothDeviceConnect;
    }

    public MutableLiveData<Boolean> getIsSetting() {
        return isSetting;
    }

    public long getSettingTime() {
        return settingTime;
    }

    public double getSettingDeviation() {
        return settingDeviation;
    }

    public MutableLiveData<Long> getProgressTimeML() {
        return progressTimeML;
    }

    public MutableLiveData<Boolean> getIsNowRecord() {
        return isNowRecord;
    }

    public MutableLiveData<String> getStartTimeML() {
        return startTimeML;
    }

    public MutableLiveData<String> getEndTimeML() {
        return endTimeML;
    }

    public interface BluetoothDeviceClickListener {
        public void onClick(String address);
    }

    public interface ConnectSuccessListener {
        public void onSuccess(BTManager.ConnectedBluetoothThread connectedBluetoothThread);

        public void onFailed(Exception e);
    }

    public interface BluetoothConnectListener {
        public void isConnected();

        public void isDisConnected(Exception e);
    }

    public interface UpdatePressListener {
        public void isUpdate(String msg);
    }


}
