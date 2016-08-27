package com.threeosix.exp.shakemotiondetection.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.adapters.BluetoothListAdapter;
import com.threeosix.exp.shakemotiondetection.helper.AccelerometerManager;
import com.threeosix.exp.shakemotiondetection.helper.CustomBluetoothManager;
import com.threeosix.exp.shakemotiondetection.helper.CompassManager;
import com.threeosix.exp.shakemotiondetection.interfaces.BluetoothListener;
import com.threeosix.exp.shakemotiondetection.interfaces.SensorListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;

public class TestingActivity extends AppCompatActivity implements SensorListener, BluetoothListener {
    private final static String TAG = "TestingActivity";
    private final static int BT_ENABLING_CODE = 306;
    private BluetoothAdapter BTAdapter;
    private CustomBluetoothManager btUtil;
    private BluetoothListAdapter btListAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayList<BluetoothDevice> newDeviceList;
    private BluetoothDevice deviceTryToPair;
    private BluetoothSocket activeSocket;
    private boolean turnOn;
    private AlertDialog shownDialog;

    private Timer timer = new Timer();
    private final long DELAY = 1000; // millisecond
    private boolean canSendMessage;
    private boolean sensorLocked;
    @BindView(R.id.message) TextView TVMessage;

    private final BroadcastReceiver BTBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        setMessageRelatedWithBluetooth(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (shownDialog.isShowing())
                                    shownDialog.dismiss();
                            }
                        });
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        setMessageRelatedWithBluetooth(true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (shownDialog==null || !shownDialog.isShowing())
                                    checkPairedDevices();
                            }
                        });

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);
        ButterKnife.bind(this);
        initialize();
    }

    private void initialize(){
        //get local/phone's bluetooth device
        turnOn = false;
        canSendMessage = true;
        sensorLocked = false;
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        newDeviceList = new ArrayList<>();
        btListAdapter = new BluetoothListAdapter(this, deviceList);
        if (BTAdapter != null) {
            setMessageRelatedWithBluetooth(BTAdapter.isEnabled());
            //register BT receiver
            IntentFilter filterBTState = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(BTBroadcastReceiver, filterBTState);
            btUtil = new CustomBluetoothManager(null);//leave register to CustomBluetoothManager
            if (BTAdapter.isEnabled())
                checkPairedDevices();

        }else
            TVMessage.setText("Your device must support bluetooth to use this app");// Device does not support Bluetooth
    }

    private void checkPairedDevices() {
        deviceList.clear();
        Set<BluetoothDevice> pairedDevice = BTAdapter.getBondedDevices();
        if(pairedDevice.size()>0) {
            deviceList.addAll(pairedDevice);
        }
        showListPopupDialog();
        //btUtil.startSearching(this,BTAdapter,this);
    }

    private void showListPopupDialog(){
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.popup_list, null);
        builderSingle.setView(convertView);
        shownDialog = builderSingle.create();
        shownDialog.setCanceledOnTouchOutside(false);
        ListView lv = (ListView) convertView.findViewById(R.id.list_content);
        lv.setAdapter(btListAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                shownDialog.dismiss();
                BluetoothDevice selectedDevice = deviceList.get(position);
                if (newDeviceList.contains(selectedDevice)) {
                    deviceTryToPair = selectedDevice;
                    btUtil.pairDevice(selectedDevice, TestingActivity.this, TestingActivity.this);
                } else {
                    try {
                        btUtil.connect(selectedDevice,TestingActivity.this);
                    } catch (IOException e) {
                        Toast.makeText(TestingActivity.this, "Unable to connect",Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        });
        shownDialog.show();
    }

    private void setMessageRelatedWithBluetooth(boolean enabled){
        String message = "";
        if (enabled)
            message = "Your bluetooth name is: "+BTAdapter.getName();
        else
            message = "Please turn on your bluetooth to use this app";

        TVMessage.setText(message);
    }

    @OnTouch(R.id.btn_lock)
    public boolean lockSensor(View v, MotionEvent event){
        sensorLocked = event.getAction() == MotionEvent.ACTION_DOWN;
        return true;
    }


    @Override
    public void onAccelerationChanged(float x, float y, float z) {

    }

    @Override
    public void onShake(float force) {
        // Do your stuff here
        if (canSendMessage && sensorLocked){
            // Called when Motion Detected
            turnOn = !turnOn;
            String message = turnOn ? "daya/" : "mati/";
//            sendMessageToBluetooth(message);
        }

    }

    @Override
    public void onCompassRotate(CompassManager.CompassDirection compassDirection, float degree) {
        String current_direction = "";
        switch (compassDirection){
            case NORTH:
                current_direction = "North";
                break;
            case EAST:
                current_direction = "East";
                break;
            case SOUTH:
                current_direction = "South";
                break;
            case WEST:
                current_direction = "West";
                break;
            case UNKNOWN:
                current_direction = "Unknown";
                break;
        }

        Log.d(TAG, "\ncurrent direction: " + current_direction);
    }

    @Override
    public void onCompassRotate(float azimuth, float pitch, float roll) {
        Log.d(TAG,"azimuth: "+azimuth+"\npitch: "+pitch+"\nroll: "+roll);
        String current_direction = "";
        if ((azimuth > -0.2) && (azimuth < 0.2)) current_direction = "North";
        if ((azimuth > 1.3) && (azimuth < 1.7)) current_direction = "East";
        if (((azimuth < -2.8) && (azimuth > -3.2)) || ((azimuth < 3.2) && (azimuth > 2.8))) current_direction = "South";
        if ((azimuth > -1.7) && (azimuth < -1.3)) current_direction = "West";
        Log.d(TAG,"\ncurrent direction: "+current_direction);
    }

    @Override
    public void onResume() {
        super.onResume();
        Toast.makeText(getBaseContext(), "onResume Accelerometer Started",
                Toast.LENGTH_SHORT).show();

        //Check device supported Accelerometer sensor or not
        if (AccelerometerManager.isSupported(this)) {

            //Start Accelerometer Listening
            AccelerometerManager.startListening(this);
        }

        //Check device supported Compass sensor or not
        if (CompassManager.isSupported(this)) {

            //Start Compass Listening
            CompassManager.startListening(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode==BT_ENABLING_CODE){
//            setMessageRelatedWithBluetooth(resultCode == RESULT_OK);
//        }//already use receiver
    }

    @Override
    public void onStop() {
        super.onStop();

        //Check device supported Accelerometer senssor or not
        if (AccelerometerManager.isListening()) {

            //Stop Accelerometer Listening
            AccelerometerManager.stopListening();

//            Toast.makeText(getBaseContext(), "onStop Accelerometer Stoped",
//                    Toast.LENGTH_SHORT).show();
        }

        if (CompassManager.isListening()) {

            //Stop Compass Listening
            CompassManager.stopListening();

//            Toast.makeText(getBaseContext(), "onStop Compass Stoped",
//                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Sensor", "Service  destroy");

        //Check device supported Accelerometer senssor or not
        if (AccelerometerManager.isListening()) {

            //Stop Accelerometer Listening
            AccelerometerManager.stopListening();

//            Toast.makeText(getBaseContext(), "onDestroy Accelerometer Stoped",
//                    Toast.LENGTH_SHORT).show();
        }

        if (CompassManager.isListening()) {

            //Stop Compass Listening
            CompassManager.stopListening();

//            Toast.makeText(getBaseContext(), "onDestroy Compass Stoped",
//                    Toast.LENGTH_SHORT).show();
        }

        unregisterReceiver(BTBroadcastReceiver);
        if (btUtil.getBTCommonReceiver()!=null)
            unregisterReceiver(btUtil.getBTCommonReceiver());

    }

    @Override
    public void onPairFinish() {
        newDeviceList.remove(deviceTryToPair);
    }

    @Override
    public void onUnpairFinish() {

    }

    @Override
    public void onFound(BluetoothDevice BTDevice) {
        //Toast.makeText(this, "BT Device ACTION_FOUND", Toast.LENGTH_SHORT).show();
        boolean notExist = true;
        for(BluetoothDevice device : deviceList) {
            if(device.getAddress().equals(BTDevice.getAddress())) {
                notExist = false;
                break;
            }
        }
        if(notExist == true) {
            newDeviceList.add(BTDevice);
            deviceList.add(BTDevice);
            btListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onConnect(BluetoothDevice BTDevice){
        activeSocket = btUtil.getActiveSocket();
    }

    private void sendMessageToBluetooth(String message){
        if (activeSocket!=null){
            try {
                Toast.makeText(getBaseContext(), "Motion detected, sending message: "+message,
                        Toast.LENGTH_SHORT).show();
                OutputStream outputStream = activeSocket.getOutputStream();
                outputStream.write(message.getBytes());
                canSendMessage = false;
                timer.cancel();
                timer = new Timer();
                timer.schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                // TODO: do what you need here (refresh list)
                                // you will probably need to use runOnUiThread(Runnable action) for some specific actions
                                canSendMessage = true;
                            }
                        },
                        DELAY
                );
            } catch (IOException e) {
                Toast.makeText(this, "Unable to send command",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

    }


}
