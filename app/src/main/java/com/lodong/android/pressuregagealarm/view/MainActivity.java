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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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

    private ProgressDialog progressDialog;

    private String nowType = "";
    private String nowValue;

    private boolean isBluetoothDeviceConnect;

    private boolean isSetting;
    private boolean isDisplaySetting;

    private final String MENT_RECORDTIME = "설정된 기록 시간 : ";
    private final String MENT_STARTTIME = "기록 시작 시간 : ";
    private final String MENT_LASTTIME = "남은 시간 : ";
    private final String MENT_ENDTIME = "기록 종료 시간 : ";
    private final double criTime = 3600000;

    //기록 관련 변수
    private boolean isRecord;
    private Thread displayRecordThread;
    private LineChart chart;


    //권한 관련 변수
    private String[] permisson1 = new String[]{Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION
            , Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS};
    private String[] permisson2 = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION
            , Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS};


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
            settingGraph();
            settingView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSetting) {
            changeSettingDisplay();
        }
        if (!isRecord) {
            binding.imgRecord.setVisibility(View.INVISIBLE);
        }
    }

    public void settingView() {
        progressDialog = new ProgressDialog(this);
        viewModel.getIsloading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading) {
                    MainActivity.this.progressDialog.show();
                } else {
                    MainActivity.this.progressDialog.dismiss();
                }
            }
        });

        viewModel.getIsBluetoothDeviceConnect().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnect) {
                if (isConnect) {
                    Toast.makeText(getApplication(), "기기와 연결이 성공적으로 되었습니다.", Toast.LENGTH_SHORT).show();
                    MainActivity.this.isBluetoothDeviceConnect = true;
                    changeSettingDisplay();
                } else {
                    MainActivity.this.isBluetoothDeviceConnect = false;
                    binding.imgSignal.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplication(), "기기와의 연결이 끊겼습니다.", Toast.LENGTH_SHORT).show();
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
        viewModel.setHandler(new BluetoothResponseHandler(this, Looper.getMainLooper()));
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
            } else if (strArrTmp.length == 2) {
                Log.d("ERROR", "********ERROR**********");
                return;
            } else {
                String press = strArrTmp[2].trim();
                String type = strArrTmp[3].trim();

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
                if (!nowType.equals(type)) {
                    this.nowType = type.trim();
                    changeSettingDisplay();
                }
                this.nowType = type.trim();
                this.nowValue = press.trim();
                addEntry(Double.parseDouble(this.nowValue));
            }
        }
    }


    private boolean requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            return PermissionCheck.checkAndRequestPermissions(this, permisson1);
        } else {
            return PermissionCheck.checkAndRequestPermissions(this, permisson2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull java.lang.String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int result = PermissionCheck.onRequestPermissionsResult(this, requestCode, permissions, grantResults);

        if (result == PermissionCheck.RESULT_GRANTED) {
            //권한 동의 완료
            settingGraph();
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
        if (isRecord) {
            Toast.makeText(getApplication(), "기록 중지 후 접근해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isBluetoothDeviceConnect) {
            Toast.makeText(getApplication(), "기기 연결 후 접근해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
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
            Log.d(TAG, deviation + "");
            binding.txtDeviation.setText("±" + deviation);
            isDisplaySetting = true;
        }

        long time = viewModel.getSettingTime();
        String displayTime = MENT_RECORDTIME + (time / criTime);
        binding.txtSettingTime.setText(displayTime + "시간");
    }

    public void recordStart() {
        if (!isBluetoothDeviceConnect) {
            displayNotConnect();
            return;
        }
        if (!isSetting) {
            Toast.makeText(getApplication(), "설정값을 세팅해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRecord) {
            Toast.makeText(getApplication(), "이미 기록중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        //기록 시작 -> RoomDB 연결 -> event가 발생할 때마다 RoomDB에 기록
        viewModel.getIsNowRecord().observe(this, isNowRecord -> {
            if (isNowRecord) {
                //현재 녹화중이라는 ui 이벤트 발생
                MainActivity.this.isRecord = true;
                displayRec();
            } else {
                MainActivity.this.isRecord = false;
                initNotRecordDisplay();
            }
        });

        viewModel.getStartTimeML().observe(this, s -> binding.txtStartTime.setText("기록 시작 시간 : " + s));

        viewModel.getEndTimeML().observe(this, s -> binding.txtEndTime.setText("기록 종료 시간 : " + s));

        viewModel.getProgressTimeML().observe(this, lastTime -> {
            long hour = (long) (lastTime / criTime);
            long min = (lastTime - (long) (hour * criTime)) / 60000;
            long sec = (lastTime - (long) (hour * criTime) - (min * 60000)) / 1000;
            Log.d(TAG, "시 " + hour + " 분 " + min + " 초 " + sec);
            binding.txtTimeRemaining.setText("남은시간 : " + String.format("%02d", hour) + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec));
        });
        viewModel.showRecordStartDialog(Double.parseDouble(this.nowValue));
    }

    private void displayRec() {
        displayRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (MainActivity.this.isRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (binding.imgRecord.getVisibility() == View.INVISIBLE) {
                                binding.imgRecord.setVisibility(View.VISIBLE);
                            } else {
                                binding.imgRecord.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        });

        displayRecordThread.start();
    }

    public void recordStop() {
        if (!isRecord) {
            Toast.makeText(getApplication(), "기록중이 아닙니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.showRecordEndDialog();
    }

    private void displayNotConnect() {
        Toast.makeText(getApplication(), "블루투스 모델이 연결되지 않았습니다.\n연결 후 기능을 이용해주세요.", Toast.LENGTH_SHORT).show();
    }

    private void initNotRecordDisplay() {
        binding.imgRecord.setVisibility(View.INVISIBLE);
        binding.txtStartTime.setText("기록 시작 시간(YYMMDD)");
        binding.txtEndTime.setText("기록 종료 시간(YYMMDD)");
        binding.txtTimeRemaining.setText("남은시간");
    }

    public void intentRecordList() {
        startActivity(new Intent(this, RecordListActivity.class));
    }

    private class ProgressDialog extends Dialog {

        public ProgressDialog(@NonNull Context context) {
            super(context);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.progressdialog);
            setCancelable(false);

            /*getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));*/
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    @Override
    protected void onDestroy() {
        if (isRecord) {
            viewModel.insertEvent("앱 강제종료 되었습니다.");
        }
        super.onDestroy();
    }

    private void settingGraph() {
        chart = binding.LineChart;

        chart.setDrawGridBackground(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setGridBackgroundColor(Color.WHITE);

// description text
        chart.getDescription().setEnabled(true);
        Description des = chart.getDescription();
        des.setEnabled(true);
        des.setText("press");
        des.setTextSize(15f);
        des.setTextColor(Color.BLACK);

// touch gestures (false-비활성화)
        chart.setTouchEnabled(false);

// scaling and dragging (false-비활성화)
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);

//auto scale
        chart.setAutoScaleMinMaxEnabled(true);

// if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

//X축
        chart.getXAxis().setDrawGridLines(true);
        chart.getXAxis().setDrawAxisLine(false);

        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setDrawGridLines(false);

//Legend
        Legend l = chart.getLegend();
        l.setEnabled(true);
        l.setFormSize(10f); // set the size of the legend forms/shapes
        l.setTextSize(12f);
        l.setTextColor(Color.BLACK);

//Y축
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(true);
        leftAxis.setTextColor(getResources().getColor(R.color.black));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getResources().getColor(R.color.black));

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);


// don't forget to refresh the drawing
        chart.invalidate();
    }

    private void addEntry(double num) {

        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        ILineDataSet set = data.getDataSetByIndex(0);
        // set.addEntry(...); // can be called as well

        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }
        data.addEntry(new Entry((float) set.getEntryCount(), (float) num), 0);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart.notifyDataSetChanged();

        chart.setVisibleXRangeMaximum(100);
        // this automatically refreshes the chart (calls invalidate())
        chart.moveViewTo(data.getEntryCount(), 50f, YAxis.AxisDependency.LEFT);
    }


    @SuppressLint("ResourceType")
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "press");
        set.setLineWidth(1f);
        set.setDrawValues(true);
        set.setValueTextColor(getResources().getColor(R.color.black));
        set.setColor(getResources().getColor(R.color.black));
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawCircles(false);
        set.setHighLightColor(Color.rgb(190, 190, 190));
        return set;
    }
}