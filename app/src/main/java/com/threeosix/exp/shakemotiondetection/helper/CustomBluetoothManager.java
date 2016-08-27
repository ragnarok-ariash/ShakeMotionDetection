package com.threeosix.exp.shakemotiondetection.helper;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.threeosix.exp.shakemotiondetection.interfaces.BluetoothListener;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by taufik on 8/27/16.
 */
public class CustomBluetoothManager {

    private static final String TAG = "CustomBluetoothManager";
    private BroadcastReceiver BTCommonReceiver;
    private BluetoothSocket mmSocket;

    public CustomBluetoothManager(BroadcastReceiver BTCommonReceiver) {
        this.BTCommonReceiver = BTCommonReceiver;
    }

    public BroadcastReceiver getBTCommonReceiver() {
        return BTCommonReceiver;
    }

    public BluetoothSocket getActiveSocket() {
        return mmSocket;
    }

    public void setBTCommonReceiver(BroadcastReceiver BTCommonReceiver) {
        this.BTCommonReceiver = BTCommonReceiver;
    }

    public void pairDevice(BluetoothDevice device, final BluetoothListener callback, Activity context) {
        try {
            if (callback!=null) {
                BroadcastReceiver receiver = getRegisterCallback(callback);
                IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                context.registerReceiver(receiver, intent);
            }

            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unpairDevice(BluetoothDevice device, BluetoothListener callback, Activity context) {
        try {
            if (callback!=null) {
                BroadcastReceiver receiver = getRegisterCallback(callback);
                IntentFilter intent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                context.registerReceiver(receiver, intent);
            }

            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect(BluetoothDevice device, BluetoothListener callback) throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        mmSocket =  device.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        if (callback!=null)
            callback.onConnect(device);
    }

    private BroadcastReceiver getRegisterCallback(final BluetoothListener callback){
        if (BTCommonReceiver==null){
            BTCommonReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                        if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                            Log.i("Log"+TAG, "finish pairing process");
                            callback.onPairFinish();
                        } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                            Log.i("Log"+TAG, "finish unpairing process");
                            callback.onUnpairFinish();
                        }

                    }else if(BluetoothDevice.ACTION_FOUND.equals(action)){
                        Log.i("Log"+TAG, "found bluetooth device");
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        callback.onFound(device);
//                    try
//                    {
//                        //device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
//                        //device.getClass().getMethod("cancelPairingUserInput", boolean.class).invoke(device);
//                    }
//                    catch (Exception e) {
//                        Log.i("Log", "Inside the exception: ");
//                        e.printStackTrace();
//                    }


                    }
                }

            };
        }

        return BTCommonReceiver;
    }

    public void startSearching(Activity context, BluetoothAdapter bluetoothAdapter, BluetoothListener callback) {
        if (context!=null && bluetoothAdapter != null){
            Log.i("Log"+TAG, "start searching bluetooth device");
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(getRegisterCallback(callback), intentFilter);
            bluetoothAdapter.startDiscovery();
        }

    }
}
