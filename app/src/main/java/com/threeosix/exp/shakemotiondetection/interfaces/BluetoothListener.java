package com.threeosix.exp.shakemotiondetection.interfaces;

import android.bluetooth.BluetoothDevice;

/**
 * Created by taufik on 8/27/16.
 */
public interface BluetoothListener {
    void onPairFinish();
    void onUnpairFinish();
    void onFound(BluetoothDevice BTDevice);
    void onConnect(BluetoothDevice BTDevice);
}
