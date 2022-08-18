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

import com.google.android.material.tabs.TabItem;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.OnReadMessageInterface;
import com.lodong.android.pressuregagealarm.R;
import com.lodong.android.pressuregagealarm.databinding.ActivityMainBinding;
import com.lodong.android.pressuregagealarm.permission.PermissionCheck;
import com.lodong.android.pressuregagealarm.viewmodel.MainViewModel;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnReadMessageInterface {
    private final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private MainViewModel viewModel;

    private String nowType = "";
    private String nowValue;

    private boolean isBluetoothDeviceConnect;

    private boolean isSetting;
    private boolean isDisplaySetting;

    private final String MENT_RECORDTIME = "설정된 기록 시간 : ";
    private final String MENT_STARTTIME ="기록 시작 시간 : ";
    private final String MENT_LASTTIME = "남은 시간 : ";
    private final String MENT_ENDTIME = "기록 종료 시간 : ";
    private final double criTime = 3600000;

    //기록 관련 변수
    private boolean isRecord;

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

    @Override
    protected void onResume() {
        super.onResume();
        if (isSetting) {
            changeSettingDisplay();
        }
    }

    public void settingView() {
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

        viewModel.getIsBluetoothDeviceConnect().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnect) {
                if (isConnect) {
                    MainActivity.this.isBluetoothDeviceConnect = true;
                    changeSettingDisplay();
                }
            }
        });

        viewModel.getIsSetting().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSetting) {
                if (isSetting) {
                    MainActivity.this.isSetting = true;
                    //set setting value
                    changeSettingDisplay();
                }
            }
        });
        viewModel.getSettingInfo();

    }

    public void showDeviceList() {
        Log.d(TAG, "showDeviceList");
        viewModel.setHandler(new BluetoothResponseHandler(this));
        viewModel.settingBluetooth();
        viewModel.showDeleteDialog();
    }

    public void changeUnit() {
        if (isBluetoothDeviceConnect) {
            viewModel.changeUnit(nowType);
            isDisplaySetting = false;
        } else {
            displayNotConnect();
        }
    }

    @Override
    public void onReadMessage(String message) {
        String value;
        StringBuilder msg = new StringBuilder();
        msg.append(message);
        /* Log.d("MSG", msg.toString() + " , " + msg.toString().length());*/
        if (msg.toString().length() >= 12 && msg.toString().length() < 23 && !msg.toString().contains("UNIT") && !msg.toString().contains("DATA")) {
            String[] strArrTmp = msg.toString().split(" ");
            if (strArrTmp.length == 3) {
                Log.d("ERROR", "********ERROR**********");
                return;
            }else if(strArrTmp.length == 2){
                Log.d("ERROR", "********ERROR**********");
                return;
            } else {
                String press = strArrTmp[2];
                String type = strArrTmp[3];

                binding.txtPressureValue.setText(press);
                binding.txtType.setText(type);
                if (binding.imgSignal.getVisibility() == View.INVISIBLE) {
                    binding.imgSignal.setVisibility(View.VISIBLE);
                } else {
                    binding.imgSignal.setVisibility(View.INVISIBLE);
                }
                if (!isDisplaySetting) {
                    changeSettingDisplay();
                }
                if(!nowType.equals(type)){
                    this.nowType = type;
                    changeSettingDisplay();
                }
                this.nowType = type;
                this.nowValue = press;
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
            settingView();
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

    public void intentSettingActivity() {
        startActivity(new Intent(this, SettingActivity.class));
    }

    private void changeSettingDisplay() {
        if (this.nowType == null && this.isSetting) {
            Log.d(TAG, "changeSettingDisplay()1");
            double deviation = viewModel.getSettingDeviation();
            binding.txtDeviation.setText("±" + deviation);
        } else if (this.isSetting) {
            Log.d(TAG, "changeSettingDisplay()2");
            viewModel.changeDeviationType(this.nowType.trim());
            double deviation = viewModel.getSettingDeviation();
            Log.d(TAG, deviation+"");
            binding.txtDeviation.setText("±" + deviation);
            isDisplaySetting = true;
        }

        long time = viewModel.getSettingTime();
        String displayTime = MENT_RECORDTIME + (time / criTime);
        binding.txtSettingTime.setText(displayTime + "시간");
    }

    private void recordStart() {
        if(!isBluetoothDeviceConnect){
            displayNotConnect();
            return;
        }
        if(!isSetting){
            Toast.makeText(getApplication(), "설정값을 세팅해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        //기록 시작 -> RoomDB 연결 -> event가 발생할 때마다 RoomDB에 기록
        isRecord = true;
    }

    private void recordStop(){
        if(!isRecord){
            Toast.makeText(getApplication(), "기록중이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        isRecord = false;
    }

    private void displayNotConnect() {
        Toast.makeText(getApplication(), "블루투스 모델이 연결되지 않았습니다.\n연결 후 기능을 이용해주세요.", Toast.LENGTH_SHORT).show();
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