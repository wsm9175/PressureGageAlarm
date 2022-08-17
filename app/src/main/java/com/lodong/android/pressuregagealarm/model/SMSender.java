package com.lodong.android.pressuregagealarm.model;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.widget.Toast;

public class SMSender {
    private final String MENT1 = "이하입니다. 확인해주세요.";
    private String tarketPhoneNumber;
    private Activity mActivity;

    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERD";

    public SMSender(Activity activity){
        this.mActivity = activity;
    }

    public void sendSMS(String info){
        String contents = info;
        startSendMessages(getTarketPhoneNumber(), contents);
    }

    private void startSendMessages(final String phoneNumber, String contents){

        SmsManager sms = SmsManager.getDefault();

        PendingIntent sentPI = PendingIntent.getBroadcast(mActivity, 0, new Intent(SENT), 0|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(mActivity, 0, new Intent(DELIVERED), 0|PendingIntent.FLAG_IMMUTABLE);

        sms.sendTextMessage(phoneNumber, null, contents , sentPI, deliveredPI);
    }

    public String getTarketPhoneNumber() {
        return tarketPhoneNumber;
    }

    public void setTarketPhoneNumber(String targetPhoneNumber) {
        this.tarketPhoneNumber = tarketPhoneNumber;
    }

    public BroadcastReceiver getSentBroadCast(){

        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        Toast.makeText(mActivity.getBaseContext(), "SMS 발송 시작", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(mActivity.getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(mActivity.getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(mActivity.getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(mActivity.getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    public BroadcastReceiver getDeliveredBroadCast(){
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()){
                    case Activity.RESULT_OK:
                        Toast.makeText(mActivity.getBaseContext(), "SMS DELIVERED", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(mActivity.getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    public IntentFilter getSentIntentFilter(){
        return new IntentFilter(SENT);
    }

    public IntentFilter getDeliveredIntentFilter(){
        return new IntentFilter(DELIVERED);
    }
}
