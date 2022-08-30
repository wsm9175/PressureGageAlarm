package com.lodong.android.pressuregagealarm.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.entity.BroadCastAction;
import com.lodong.android.pressuregagealarm.entity.EventEntity;
import com.lodong.android.pressuregagealarm.model.GMailSender;
import com.lodong.android.pressuregagealarm.model.SMSender;
import com.lodong.android.pressuregagealarm.model.TextFileMaker;
import com.lodong.android.pressuregagealarm.module.BTManager;
import com.lodong.android.pressuregagealarm.roomDB.EventListInterface;
import com.lodong.android.pressuregagealarm.view.MainActivity;
import com.lodong.android.pressuregagealarm.viewmodel.MainViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

public class RecordService extends Service {
    private final String TAG = RecordService.class.getSimpleName();
    NotificationManager Notifi_M;

    private long settingTime;
    private double settingDeviation;
    private List<String> settingPhoneNumber;
    private List<String> settingEmailList;
    private String settingMessageMent;

    private final String NOSETTING = "설정되지 않음";

    //녹화 관련 변수
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

    private static boolean isRecord = true;

    private TextFileMaker textFileMaker;

    private final String FOLDERNAME = "DEA압력계";
    private String saveFileName;


    public RecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //init Value
        Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        this.settingTime = intent.getLongExtra("settingTime", 0);
        this.settingDeviation = intent.getDoubleExtra("settingDeviation", 0);
        this.settingPhoneNumber = intent.getStringArrayListExtra("settingPhoneNumber");
        this.settingEmailList = intent.getStringArrayListExtra("settingEmailList");
        String messageMent = intent.getStringExtra("settingMessageMent");
        if(messageMent.equals(NOSETTING)){
            messageMent = "";
        }else{
            messageMent += "\n";
        }
        this.settingMessageMent = messageMent;

        Double nowValue = intent.getDoubleExtra("nowValue", 0);

        BTManager.getInstance().setRecordBluetoothListener(getBluetoothConnectListener());
        recordStart(nowValue);
        fileSetting();
        startMyOwnForeground();

        return START_STICKY;
    }


    private void recordStart(double nowValue) {
        //init
        this.progressTime = 0;
        errorCount = 0;

        //편차값, 측정 시간, test 완료 여부, 베터리 20프로, 블루투스 연결 disconnect시, 타이머, 에러, 앱종료
        this.startPress = nowValue;

        //측정 시간
        startRecordTimer();

        //압력값 계산 및 값 에러
        BTManager.getInstance().setUpdateListener(updatePressListener());
        //블루투스 연결 disconnect시 -> 선언된 connect callback에서 감시 후 처리.

        /* broadcast 부분 - 배터리 및 앱 강제 종료시*/
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.BATTERY_LOW")) {
                    startSendEmail(settingMessageMent+LOW_BATTERY_MESSAGE);
                    startSendMessage(settingMessageMent+LOW_BATTERY_MESSAGE);
                    insertEvent(settingMessageMent+LOW_BATTERY_MESSAGE);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        registerReceiver(broadcastReceiver, filter);
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
                            startSendMessage(settingMessageMent+message);
                            startSendEmail(settingMessageMent+message);
                            endRecord();
                        } else {
                            progressTime += 1000;
                            Intent intent = new Intent();
                            intent.setAction(BroadCastAction.PROGRESS_TIME);
                            intent.putExtra(BroadCastAction.PROGRESS_TIME, progressTime);
                            sendBroadcast(intent);
                        }
                    });
                }
            }, 0, 1000);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
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
                startSendMessage(settingMessageMent+message);
                startSendEmail(settingMessageMent+message);
                insertEvent(settingMessageMent+message);
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
                startSendMessage(settingMessageMent+message);
                startSendEmail(settingMessageMent+message);
                insertEvent(settingMessageMent+message);
            }
        }
    }

    public void insertEvent(String event) {
        long occurTime = System.currentTimeMillis();
        EventEntity entity = new EventEntity(event, occurTime);
        EventListInterface eventListInterface = new EventListInterface(this.getApplication());
        eventListInterface.insert(entity);
    }

    private void checkError() {
        if (errorCount >= 3) {
            startSendMessage(settingMessageMent+ERROR_MESSAGE);
            startSendEmail(settingMessageMent+ERROR_MESSAGE);
            insertEvent(settingMessageMent+ERROR_MESSAGE);
        }
    }

    private void startSendMessage(final String message) {
        if (this.settingPhoneNumber != null) {
            if (this.settingPhoneNumber.size() != 0) {
                SMSender smSender = new SMSender(this);
                registerReceiver(smSender.getSentBroadCast(), smSender.getSentIntentFilter());
                registerReceiver(smSender.getDeliveredBroadCast(), smSender.getDeliveredIntentFilter());
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
    private void fileSetting(){
        textFileMaker = new TextFileMaker();
        textFileMaker.fileSetting(FOLDERNAME, getFileName());
    }

    private void writeTxt(String data, String type){
        if(textFileMaker != null){
            String writeData = getNowTime() + " " + "값 : " + data + "단위 : " + type + "\n\n";
            textFileMaker.writeText(writeData);
        }
    }

    private void endRecord() {
        this.timer.cancel();
        isRecord = false;
        BTManager.getInstance().setUpdateListener(null);
        Intent intent = new Intent();
        intent.setAction(BroadCastAction.END_RECORD);
        sendBroadcast(intent);
        stopSelf();
    }

    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private MainViewModel.UpdatePressListener updatePressListener() {
        return s -> {
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
                    writeTxt(press, type);
                }
            }
        };
    }

    private MainViewModel.BluetoothConnectListener getBluetoothConnectListener() {
        return new MainViewModel.BluetoothConnectListener() {
            @Override
            public void isConnected() {

            }

            @Override
            public void isDisConnected(Exception e) {
                startSendEmail(settingMessageMent+DISCONNECT_MESSAGE);
                startSendMessage(settingMessageMent+DISCONNECT_MESSAGE);
                insertEvent(settingMessageMent+DISCONNECT_MESSAGE);
                endRecord();
                stopSelf();
            }
        };
    }

    private String getFileName(){
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMdd_kmmss");
        Date mDate;

        long mNow = System.currentTimeMillis();
        mDate = new Date(mNow);

        return "m"+mFormat.format(mDate)+".txt";
    }

    private String getNowTime(){
        SimpleDateFormat mFormat = new SimpleDateFormat("yy년 MM월 dd일 k시 mm분 ss초");
        Date mDate;

        long mNow = System.currentTimeMillis();
        mDate = new Date(mNow);

        return mFormat.format(mDate);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (isRecord) {
            endRecord();
            insertEvent("테스트중 기록이 중지 되었습니다.");
        }
        if(textFileMaker != null){
            textFileMaker.closeText();
        }
        super.onDestroy();
    }
}
