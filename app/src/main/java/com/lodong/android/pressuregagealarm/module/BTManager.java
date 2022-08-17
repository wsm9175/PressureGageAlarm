package com.lodong.android.pressuregagealarm.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.lodong.android.pressuregagealarm.BluetoothResponseHandler;
import com.lodong.android.pressuregagealarm.view.MainActivity;
import com.lodong.android.pressuregagealarm.viewmodel.MainViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BTManager {
    private final String TAG = BTManager.class.getSimpleName();
    private static BTManager instance;
    private Activity activity;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> arrayList = new ArrayList<>();
    private MutableLiveData<ArrayList<BluetoothDevice>> mtList = new MutableLiveData<>();

    private BluetoothSocket serverSocket;

    private boolean isConnect;
    private ConnectedBluetoothThread connectedBluetoothThread;
    private Handler mBluetoothHandler;
    private Handler settingBluetoothHandler;

    private MainViewModel.ConnectSuccessListener connectSuccessListener;
    private MainViewModel.BluetoothConnectListener connectListener;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;

    private final static int REQUEST_ENABLE_BT = 1;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "find device");
                String name = device.getName();
                if (name != null) {
                    arrayList.add(device);
                    mtList.setValue(arrayList);
                }
            }
        }
    };

    private BTManager() { }

    public static BTManager getInstance(){
        if(instance == null){
            instance = new BTManager();
        }

        return instance;
    }

    public void init(Activity activity, MainViewModel.ConnectSuccessListener listener, BluetoothResponseHandler handler, MainViewModel.BluetoothConnectListener bluetoothConnectListener){
        this.activity = activity;
        bluetoothManager = activity.getSystemService(BluetoothManager.class);
        this.mtList.setValue(arrayList);
        this.connectSuccessListener = listener;
        this.mBluetoothHandler = handler;
        this.connectListener = bluetoothConnectListener;
    }

    //adapter 세팅
    public boolean settingBT() {
        btAdapter = bluetoothManager.getAdapter();
        if (btAdapter == null) {
            //Device doesn't support Bluetooth
            return false;
        }
        return true;
    }

    //블루투스 활성화
    @SuppressLint("MissingPermission")
    public void enabledBT() {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @SuppressLint("MissingPermission")
    public void getBtDevice() {
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, device.getName());
                arrayList.add(device);
                this.mtList.setValue(arrayList);
            }
        }

        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
            Log.d(TAG, "btAdapter.cancelDiscovery()");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "btAdapter.isEnabled()");
                btAdapter.startDiscovery();
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                activity.registerReceiver(receiver, intentFilter);
            } else {
                Toast.makeText(activity, "bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public ArrayList<BluetoothDevice> getBtDeviceList() {
        return this.arrayList;
    }

    public MutableLiveData<ArrayList<BluetoothDevice>> getMtList() {
        return mtList;
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(String address) {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        ParcelUuid[] uuids = null;
        try {
            uuids = (ParcelUuid[]) device.getClass().getMethod("getUuids", null).invoke(device, null);
            if (uuids != null) {
                for (ParcelUuid uuid : uuids) {
                    Log.d(TAG, device.getName() + ": " + uuid.toString());
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Log.d(TAG, device.getName());
        boolean flag = true;
        try {
            serverSocket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
            serverSocket.connect();
            this.connectListener.isConnected();
        } catch (IOException e) {
            flag = false;
            e.printStackTrace();
        }

        if (flag) {
            this.isConnect = true;
            connectedBluetoothThread = new ConnectedBluetoothThread(serverSocket);
            connectedBluetoothThread.start();
            connectSuccessListener.onSuccess(connectedBluetoothThread);
        }
    }

    public class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(activity, "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
                connectSuccessListener.onFailed(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()
            StringBuilder readMessage = new StringBuilder();

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    /*// Read from the InputStream.
                    SystemClock.sleep(100);
                    numBytes = mmInStream.read(mmBuffer);
                    final String readingMessage = new String(mmBuffer, "US-ASCII");
                    if (readingMessage.contains("\n")) {
                        Log.d(TAG, "read message : " + readingMessage);
                    }
                    // Send the obtained bytes to the UI activity.*/
                    int bytes = this.mmInStream.read(mmBuffer);
                    String readed = new String(mmBuffer, 0, bytes);
                    readMessage.append(readed);
                    if (readed.contains("\n")) {
                        BTManager.this.mBluetoothHandler.obtainMessage(2, bytes, -1, readMessage.toString()).sendToTarget();
                        if (BTManager.this.settingBluetoothHandler != null) {
                            BTManager.this.settingBluetoothHandler.obtainMessage(2, bytes, -1, readMessage.toString()).sendToTarget();
                        }
                        Log.d(TAG, readMessage.toString());
                        readMessage.setLength(0);
                    }

                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    BTManager.this.connectListener.isDisConnected(e);
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(activity, "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(activity, "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public Handler getSettingBluetoothHandler() {
        return settingBluetoothHandler;
    }

    public void setSettingBluetoothHandler(Handler settingBluetoothHandler) {
        this.settingBluetoothHandler = settingBluetoothHandler;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }
}
