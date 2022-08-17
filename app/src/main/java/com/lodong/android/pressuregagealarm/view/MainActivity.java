package com.lodong.android.pressuregagealarm.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.databinding.ActivityMainBinding;
import com.lodong.android.pressuregagealarm.permission.PermissionCheck;
import com.lodong.android.pressuregagealarm.viewmodel.MainViewModel;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ProgressDialog progressDialog;

    private String nowType;
    private String nowValue;

    //권한 관련 변수
    private String[] permisson = new String[]{Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION
            , Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setActivity(this);
        viewModel = new ViewModelProvider(this, (ViewModelProvider.Factory)
                new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MainViewModel.class);
        viewModel.setParent(this);

        binding.imgSignal.setVisibility(View.INVISIBLE);

        if (requestPermissions()) {
            settingView();
        }
    }

    public void settingView(){
      /*  progressDialog = new ProgressDialog(this);
        viewModel.getIsloading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if(isLoading){
                    MainActivity.this.progressDialog.show();
                }else{
                    MainActivity.this.progressDialog.dismiss();
                }
            }
        });*/

    }

    public void showDeviceList() {
        Log.d(TAG, "showDeviceList");
        viewModel.setHandler(new BluetoothResponseHandler(this));
        viewModel.settingBluetooth();
        viewModel.showDeleteDialog();
    }

    public void changeUnit(){
        viewModel.changeUnit(nowType);
    }

    public class BluetoothResponseHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public BluetoothResponseHandler(MainActivity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        public void setTarget(MainActivity target) {
            this.mActivity.clear();
            this.mActivity = new WeakReference<>(target);
        }

        public void handleMessage(Message msg) {
            MainActivity activity = this.mActivity.get();
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
                            activity.onReadMessage(readMessage);
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

    public void onReadMessage(String message) {
        String value;
        StringBuilder msg = new StringBuilder();
        msg.append(message);
        Log.d("MSG", msg.toString() + " , " + msg.toString().length());
        if (msg.toString().length() >= 12 && msg.toString().length() < 23 && !msg.toString().contains("UNIT") && !msg.toString().contains("DATA")) {
            String[] strArrTmp = msg.toString().split(" ");
            if (strArrTmp.length == 3) {
                Log.d("ERROR", "********ERROR**********");
                return;
            } else {
                String press = strArrTmp[2];
                String type = strArrTmp[3];
                this.nowType = type;
                this.nowValue = press;
                Log.d(TAG, "press : " + press);
                Log.d(TAG, "type : " + type);

                binding.txtPressureValue.setText(press);
                binding.txtType.setText(type);
                if (binding.imgSignal.getVisibility() == View.INVISIBLE) {
                    binding.imgSignal.setVisibility(View.VISIBLE);
                }else{
                    binding.imgSignal.setVisibility(View.INVISIBLE);
                }
            }
        }
    }


    private boolean requestPermissions() {
        return PermissionCheck.checkAndRequestPermissions(this, permisson);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull java.lang.String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int result = PermissionCheck.onRequestPermissionsResult(this, requestCode, permissions, grantResults);

        if (result == PermissionCheck.RESULT_GRANTED) {
            //권한 동의 완료
        } else if (result == PermissionCheck.RESULT_NOT_GRANTED) {
            showDialogOK(getString(R.string.permission_not_allow),
                    (dialog, which) -> {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                requestPermissions();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                finish();
                                break;
                        }
                    });
        } else if (result == PermissionCheck.RESULT_DENIED) {

            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG)
                    .show();
            /* finish();*/
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private class ProgressDialog extends Dialog {

        public ProgressDialog(@NonNull Context context) {
            super(context);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.progressdialog);
            setCancelable(false);

            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }
}