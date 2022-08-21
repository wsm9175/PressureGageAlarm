package com.lodong.android.pressuregagealarm.model;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class SMSender {
    private final String MENT1 = "이하입니다. 확인해주세요.";
    private ArrayList<String> targetList;
    private Activity mActivity;

    private int mMessageSentParts;
    private int mMessageSentCount;
    private int mMessageSentTotalParts;
    final String SENT = "SMS_SENT";
    final String DELIVERED = "SMS_DELIVERD";

    private String message;

    public SMSender(Activity activity){
        this.mActivity = activity;
    }

    public void startSendMessages(String message){
        mMessageSentCount = 0;
        sendSMS(targetList.get(mMessageSentCount),message);
    }

    public void sendSMS(final String phoneNumber, String message){
        String contents = message;
        this.message = message;
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message);
        mMessageSentTotalParts = parts.size();

        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();

        PendingIntent sentPI = PendingIntent.getBroadcast(mActivity, 0, new Intent(SENT), 0|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(mActivity, 0, new Intent(DELIVERED), 0|PendingIntent.FLAG_IMMUTABLE);

        for (int j = 0; j < mMessageSentTotalParts; j++) {
            sentIntents.add(sentPI);
            deliveryIntents.add(deliveredPI);
        }
        mMessageSentParts = 0;
        sms.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
    }


    public ArrayList<String> getTarketPhoneNumber() {
        return targetList;
    }

    public void setTarketPhoneNumber(ArrayList<String> targetPhoneNumber) {
        this.targetList = targetPhoneNumber;
    }
    private void sendNextMessage(String message){
        if(thereAreSmsToSend()){
            sendSMS(targetList.get(mMessageSentCount), message);
        }else{
            Toast.makeText(mActivity, "SMS 전송 완료하였습니다.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private boolean thereAreSmsToSend(){
        return mMessageSentCount < targetList.size();
    }
    public BroadcastReceiver getSentBroadCast(){

        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:

                        mMessageSentParts++;
                        if ( mMessageSentParts == mMessageSentTotalParts ) {
                            mMessageSentCount++;
                            sendNextMessage(message);
                        }

                        Toast.makeText(mActivity, "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(mActivity, "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(mActivity, "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(mActivity, "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(mActivity, "Radio off",
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
