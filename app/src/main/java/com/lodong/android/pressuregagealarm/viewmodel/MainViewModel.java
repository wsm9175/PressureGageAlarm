package com.lodong.android.pressuregagealarm.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.os.Handler;
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
import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.entity.SettingEntity;
import com.lodong.android.pressuregagealarm.model.GMailSender;
import com.lodong.android.pressuregagealarm.model.SMSender;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.roomDB.EventListInterface;
import com.lodong.android.pressuregagealarm.roomDB.SettingListInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

public class MainViewModel extends ViewModel {
    private final String TAG = MainViewModel.class.getSimpleName();
    private WeakReference<Activity> mRef;
    private BTManager btManager;
    private AlertDialog alertDialog;

    private MutableLiveData<Boolean> isBluetoothDeviceConnect = new MutableLiveData<>();
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

    private final double[] VALUE_PSI = {1.422339, 4.267018, 7.111696};
    private final double[] VALUE_BAR = {0.098067, 0.294199, 0.490332};
    private final double[] VALUE_KGF = {0.1, 0.3, 0.5};

    //녹화 관련 변수
    private MutableLiveData<Boolean> isNowRecord = new MutableLiveData<>();
    private MutableLiveData<Long> progressTimeML = new MutableLiveData<>();
    private MutableLiveData<String> startTimeML = new MutableLiveData<>();
    private MutableLiveData<String> endTimeML = new MutableLiveData<>();
    private double startPress;
    private long progressTime = 0;

    private final String GMAIL = "wsm9175@gmail.com";
    private final String PWD = "fdxwjxrjajntcuok";

    private Timer timer;

    private BroadcastReceiver broadcastReceiver;

    private static final long sendCriTime = 60000;
    private static long sendDeviationMessageTime = 0;
    private static int errorCount = 0;
    private static final String ERROR_MESSAGE = "압력계로 부터 오류값이 3번 이상 전달되었습니다.";
    private static final String DISCONNECT_MESSAGE = "압력계와 연결이 해제되었습니다.";
    private static final String LOW_BATTERY_MESSAGE = "배터리 잔량이 20%이하입니다.";


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
        this.progressTime = 0;
        errorCount = 0;

        long nowTime = System.currentTimeMillis();
        String recordStartTime = getTime(nowTime);
        String recordEnd = getTime(nowTime + this.settingTime);

        this.startTimeML.setValue(recordStartTime);
        this.endTimeML.setValue(recordEnd);

        //편차값, 측정 시간, test 완료 여부, 베터리 20프로, 블루투스 연결 disconnect시, 타이머, 에러, 앱종료
        isNowRecord.setValue(true);
        this.startPress = nowValue;

        //측정 시간
        startRecordTimer();

        //압력값 계산 및 값 에러
        this.btManager.setUpdateListener(getUpdatePressListener());

        //블루투스 연결 disconnect시 -> 선언된 connect callback에서 감시 후 처리.

        /* broadcast 부분 - 배터리 및 앱 강제 종료시*/
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BATTERY_LOW")) {
                    startSendEmail(LOW_BATTERY_MESSAGE);
                    startSendMessage(LOW_BATTERY_MESSAGE);
                    MainViewModel.this.insertEvent(LOW_BATTERY_MESSAGE);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        mRef.get().registerReceiver(broadcastReceiver, filter);

    }

    private void calDeviation(double nowPress) {
        double nowDiff = this.startPress - nowPress;
        if (nowPress < nowDiff) {
            //편차보다 떨어진 경우
            final String message = "압력계의 압력값이 편차값 범위를 벗어났습니다.\n" +
                    "기록 시작 압력값 : " + this.startPress + "\n" +
                    "현재 기록된 압력값 : " + nowPress + "\n" +
                    "설정된 편차값 : " + this.settingDeviation + "\n" +
                    "시작 압력값과 현재 기록된 압력값 차이 : " + nowDiff;
            Log.d(TAG, message);
            if (System.currentTimeMillis() - sendDeviationMessageTime > sendCriTime) {
                sendDeviationMessageTime = System.currentTimeMillis();
                startSendMessage(message);
                startSendEmail(message);
                MainViewModel.this.insertEvent(message);
            }
        } else if (nowPress > this.startPress + this.settingDeviation) {
            //편차보다 높아진 경우
            final String message = "압력계의 압력값이 편차값 범위를 벗어났습니다.\n" +
                    "기록 시작 압력값 : " + this.startPress + "\n" +
                    "현재 기록된 압력값 : " + nowPress + "\n" +
                    "설정된 편차값 : " + this.settingDeviation + "\n" +
                    "시작 압력값과 현재 기록된 압력값 차이 : " + nowDiff;
            Log.d(TAG, message);
            if (System.currentTimeMillis() - sendDeviationMessageTime > sendCriTime) {
                sendDeviationMessageTime = System.currentTimeMillis();
                startSendMessage(message);
                startSendEmail(message);
                MainViewModel.this.insertEvent(message);
            }
        }
    }

    private void checkError() {
        if (errorCount >= 3) {
            startSendMessage(ERROR_MESSAGE);
            startSendEmail(ERROR_MESSAGE);
            MainViewModel.this.insertEvent(ERROR_MESSAGE);
        }
    }

    private void startRecordTimer() {
        timer = new Timer();
        Handler handler = new Handler();
        final String message = "기록시간이 만료되어 기록이 정지 됐습니다.";
        try {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(() -> {
                        if (progressTime >= settingTime) {
                            //경과 시간 지남
                            startSendMessage(message);
                            startSendEmail(message);
                            endRecord();
                        } else {
                            progressTime += 1000;
                            MainViewModel.this.getProgressTimeML().setValue(MainViewModel.this.settingTime - progressTime);
                        }
                    });
                }
            }, 0, 1000);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void startSendMessage(final String message) {
        if (this.settingPhoneNumber != null) {
            if (this.settingPhoneNumber.size() != 0) {
                SMSender smSender = new SMSender(mRef.get());
                mRef.get().registerReceiver(smSender.getSentBroadCast(), smSender.getSentIntentFilter());
                mRef.get().registerReceiver(smSender.getDeliveredBroadCast(), smSender.getDeliveredIntentFilter());
                smSender.setTarketPhoneNumber((ArrayList<String>) this.settingPhoneNumber);
                smSender.startSendMessages(message);
            }
        }
    }

    private void startSendEmail(final String message) {
        if (this.settingEmailList != null) {
            /* Toast.makeText(mRef.get(), "이메일을 발송합니다.", Toast.LENGTH_SHORT).show();*/
            for (String email : this.settingEmailList) {
                Log.d(TAG, "send email : " + email);
                MailThread mailThread = new MailThread(message, email);
                mailThread.start();
            }
        }
    }

    //메일 보내는 쓰레드
    class MailThread extends Thread {
        private String message;
        private String email;

        private MailThread(String message, String email) {
            this.message = message;
            this.email = email.trim();
        }

        public void run() {
            GMailSender gMailSender = new GMailSender(GMAIL, PWD);
            //GMailSender.sendMail(제목, 본문내용, 받는사람);
            try {
                gMailSender.sendMail("PDR 500 압력계 알림 발송", message, email);
            } catch (SendFailedException e) {

            } catch (MessagingException e) {
                System.out.println("인터넷 문제" + e);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void insertEvent(String event) {
        long occurTime = System.currentTimeMillis();
        EventEntity entity = new EventEntity(event, occurTime);
        EventListInterface eventListInterface = new EventListInterface(mRef.get().getApplication());
        eventListInterface.insert(entity);
    }

    private void endRecord() {
        this.isNowRecord.postValue(false);

        this.timer.cancel();

        this.startTimeML.postValue("00:00:00");
        this.endTimeML.postValue("00:00:00");

        BTManager.getInstance().setUpdateListener(null);

        try {
            //TODO [브로드캐스트 해제]
            if (this.broadcastReceiver != null) {
                mRef.get().unregisterReceiver(broadcastReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                // 측정중이였다면
                if(MainViewModel.this.isNowRecord.getValue()!=null){
                    if (MainViewModel.this.isNowRecord.getValue()) {
                        startSendEmail(DISCONNECT_MESSAGE);
                        startSendMessage(DISCONNECT_MESSAGE);
                        insertEvent(DISCONNECT_MESSAGE);
                        endRecord();
                    }
                }
            }
        };
    }

    public UpdatePressListener getUpdatePressListener() {
        return s -> {
            if (MainViewModel.this.isNowRecord.getValue()) {
                Log.d(TAG, "getUpdatePressListener");
                //편차값
                StringBuilder msg = new StringBuilder();
                msg.append(s);
                /* Log.d("MSG", msg.toString() + " , " + msg.toString().length());*/
                if (msg.toString().length() >= 12 && msg.toString().length() < 23 && !msg.toString().contains("UNIT") && !msg.toString().contains("DATA")) {
                    String[] strArrTmp = msg.toString().split(" ");
                    if (strArrTmp.length == 3) {
                        Log.d("ERROR", "********ERROR**********");
                        errorCount += 1;
                        checkError();
                        return;
                    } else if (strArrTmp.length == 2) {
                        Log.d("ERROR", "********ERROR**********");
                        errorCount += 1;
                        checkError();
                        return;
                    } else {
                        String press = strArrTmp[2].trim();
                        String type = strArrTmp[3].trim();
                        calDeviation(Double.parseDouble(press));
                    }
                }
            }
        };
    }

    private String getTime(long time) {
        Date mDate;
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        mDate = new Date(time);
        return mFormat.format(mDate);
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
