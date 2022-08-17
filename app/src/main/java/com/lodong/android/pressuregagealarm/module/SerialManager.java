package com.lodong.android.pressuregagealarm.module;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class SerialManager implements SerialInputOutputManager.Listener {
    private final String TAG = SerialManager.class.getSimpleName();
    private Context context;

    //센서 관련 변수
    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort port;
    private static final String ACTION_USB_PERMISSION =
            "com.lodong.android.pressuregagealarm.USB_PERMISSION";
    private UsbManager usbManager;
    private PendingIntent permissionIntent;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication

                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    public SerialManager(Context context) {
        this.context = context;
    }

    public void settingSerial() {
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this.context, 0, new Intent(ACTION_USB_PERMISSION), 0 | PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);
    }

    private void connectSensor() {
        Log.d(TAG, "settingSensor");

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.usbManager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = this.usbManager.openDevice(driver.getDevice());
        if (connection == null) {
            return;
        }
        port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();
        Log.d(TAG, "usb start");
    }

    private void writeData(String data) {
        try {
            port.write("[001] GET MDAUTO\n".getBytes(), 100);
            Log.d(TAG, "write");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewData(byte[] data) {
        Log.d(TAG, "read : " + new String(data));
    }

    @Override
    public void onRunError(Exception e) {

    }
}
