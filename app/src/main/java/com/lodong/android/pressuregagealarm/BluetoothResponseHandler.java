package com.lodong.android.pressuregagealarm;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lodong.android.pressuregagealarm.view.MainActivity;

import java.lang.ref.WeakReference;

public class BluetoothResponseHandler extends Handler {
    private WeakReference<Activity> mActivity;

    public BluetoothResponseHandler(Activity activity, @NonNull Looper looper) {
        super(looper);
        this.mActivity = new WeakReference<>(activity);
    }

    public void setTarget(MainActivity target) {
        this.mActivity.clear();
        this.mActivity = new WeakReference<>(target);
    }

    public void handleMessage(Message msg) {
        Activity activity = this.mActivity.get();
        if (activity != null) {
            switch (msg.what) {
                case 1:
                    switch (msg.arg1) {
                        case 1:
                        default:
                            return;
                        case 2:

                    }
                case 2:
                    String readMessage = (String) msg.obj;
                    if (readMessage != null) {
                        ((OnReadMessageInterface)activity).onReadMessage(readMessage);
                        return;
                    }
                    return;
                case 3:
                case 4:
                case 5:
                default:
                    return;
                case 6:
                    return;
            }
        }
    }
}

