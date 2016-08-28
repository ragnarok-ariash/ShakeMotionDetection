package com.threeosix.exp.shakemotiondetection.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.adapters.BluetoothListAdapter;
import com.threeosix.exp.shakemotiondetection.helper.AccelerometerManager;
import com.threeosix.exp.shakemotiondetection.helper.CompassManager;
import com.threeosix.exp.shakemotiondetection.helper.CustomBluetoothManager;
import com.threeosix.exp.shakemotiondetection.helper.DeviceDataManager;
import com.threeosix.exp.shakemotiondetection.interfaces.BluetoothListener;
import com.threeosix.exp.shakemotiondetection.interfaces.SensorListener;
import com.threeosix.exp.shakemotiondetection.models.DeviceModel;
import com.threeosix.exp.shakemotiondetection.utils.SharedPrefsUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

public class MainControlActivity extends AppCompatActivity implements SensorListener, BluetoothListener {
    private final static String TAG = "MainControlActivity";
    private final static int BT_ENABLING_CODE = 306;
    private BluetoothAdapter BTAdapter;
    private CustomBluetoothManager btUtil;
    private BluetoothListAdapter btListAdapter;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;
    private ArrayList<BluetoothDevice> newDeviceList;
    private ArrayList<DeviceModel> controlDevices;
    private BluetoothDevice deviceTryToPair;
    private BluetoothSocket activeSocket;
    private boolean turnOn;
    private AlertDialog shownDialog;
    private DeviceDataManager deviceManager;
    private float currentDegree;
    private float[] storedDegree;
    @BindString(R.string.control_message)
    String defaultControlMessage;

    private Timer timer = new Timer();
    private final long DELAY = 200; // millisecond
    private final long CALIBRATION_DELAY = 3000; // millisecond
    private final long LOCK_DELAY = 200;
    private final int STORED_DEGREE_LENGTH = 2;
    private final int TRESHOLD_DEGREE = 20;
    private final int PARAMETER_INCREMENT = 25;
    private boolean canSendMessage;
    private boolean sensorLocked;
    private DeviceModel toBeControlledDevice;
    private Handler lockHandler;
    private Runnable lockRunnable;
    @BindView(R.id.text_control_mesage)
    TextView TVMessage;
    @BindView(R.id.progress_loading)
    ProgressBar progressBar;

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
        setContentView(R.layout.activity_main_control);
        ButterKnife.bind(this);
        initialize();
    }

    private void initialize(){
        //get local/phone's bluetooth device
        turnOn = false;
        canSendMessage = true;
        sensorLocked = false;
        currentDegree = 0.f;
        storedDegree = new float[STORED_DEGREE_LENGTH];
        deviceManager = new DeviceDataManager(this);
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        controlDevices = deviceManager.getAllDevices();
        bluetoothDeviceList = new ArrayList<>();
        newDeviceList = new ArrayList<>();
        btListAdapter = new BluetoothListAdapter(this, bluetoothDeviceList);
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
        bluetoothDeviceList.clear();
        Set<BluetoothDevice> pairedDevice = BTAdapter.getBondedDevices();
        if(pairedDevice.size()>0) {
            bluetoothDeviceList.addAll(pairedDevice);
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
                BluetoothDevice selectedDevice = bluetoothDeviceList.get(position);
                if (newDeviceList.contains(selectedDevice)) {
                    deviceTryToPair = selectedDevice;
                    btUtil.pairDevice(selectedDevice, MainControlActivity.this, MainControlActivity.this);
                } else {
                    try {
                        btUtil.connect(selectedDevice,MainControlActivity.this);
                    } catch (IOException e) {
                        Toast.makeText(MainControlActivity.this, "Unable to connect", Toast.LENGTH_SHORT).show();
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
            message = defaultControlMessage;
        else
            message = "Please turn on your bluetooth to use this app";

        TVMessage.setText(message);
    }

    private void showCalibratingDialog(){
        boolean positionCalibrated = SharedPrefsUtils.getBooleanPreference(this,"calibrating_done", false);
        if (!positionCalibrated){
            final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View convertView = (View) inflater.inflate(R.layout.popup_loading_indeterminate, null);
            builderSingle.setView(convertView);
            final AlertDialog dialog = builderSingle.create();
            dialog.setCanceledOnTouchOutside(false);
            TextView tvLoadigMsg = (TextView) convertView.findViewById(R.id.text_loading_message);
            tvLoadigMsg.setText("Calibrating position, please wait a moment...");
            dialog.show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    SharedPrefsUtils.setBooleanPreference(MainControlActivity.this, "calibrating_done", true);
                }
            }, CALIBRATION_DELAY);
        }


    }

    @OnTouch(R.id.button_lock_control)
    public boolean lockSensor(View v, MotionEvent event) {
        if (lockHandler == null)
            lockHandler = new Handler();
        if (lockRunnable == null){
            lockRunnable = new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                    sensorLocked = true;
                    storedDegree[0] = currentDegree;
                    getControlDevice();
                    //Toast.makeText(MainControlActivity.this,"now shake your device: "+currentDegree, Toast.LENGTH_SHORT).show();
                }
            };
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            //have a sec delay to lock position degree
            if (progressBar.getVisibility() == View.INVISIBLE && !sensorLocked){
                progressBar.setVisibility(View.VISIBLE);
                lockHandler.postDelayed(lockRunnable, LOCK_DELAY);
            }

        }else if (event.getAction() == MotionEvent.ACTION_UP){
            progressBar.setVisibility(View.INVISIBLE);
            sensorLocked = false;
            lockHandler.removeCallbacks(lockRunnable);
        }
        return true;
    }

//    @OnClick(R.id.button_lock_control)
//    public void onLockControl(){
//        storedDegree[0] = currentDegree;
//        sensorLocked = true;
//        getControlDevice();
//    }

    private void getControlDevice(){
        toBeControlledDevice = null;
        for (DeviceModel device : controlDevices){
            float lowerBoundDegree = device.getDeviceDegree() - TRESHOLD_DEGREE;
            float upperBoundDegree = device.getDeviceDegree() + TRESHOLD_DEGREE;
            if (lowerBoundDegree<0)
                lowerBoundDegree = 360 - Math.abs(lowerBoundDegree);
            if (upperBoundDegree > 360)
                upperBoundDegree = (upperBoundDegree - 360);

            float refDegree = currentDegree;//storedDegree[0]
            if (lowerBoundDegree > upperBoundDegree){
                if (refDegree >= lowerBoundDegree || refDegree <= upperBoundDegree){
                    toBeControlledDevice = device;
                    break;
                }
            }else{
                if (refDegree >= lowerBoundDegree && refDegree <= upperBoundDegree){
                    toBeControlledDevice = device;
                    break;
                }
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check device supported Accelerometer sensor or not
        if (AccelerometerManager.isSupported(this)) {

            //Start Accelerometer Listening
            AccelerometerManager.startListening(this);
        }

        //Check device supported Compass sensor or not
        if (CompassManager.isSupported(this)) {

            //Start Compass Listening
            CompassManager.startListening(this);
            showCalibratingDialog();
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
        boolean notExist = true;
        for(BluetoothDevice device : bluetoothDeviceList) {
            if(device.getAddress().equals(BTDevice.getAddress())) {
                notExist = false;
                break;
            }
        }
        if(notExist == true) {
            newDeviceList.add(BTDevice);
            bluetoothDeviceList.add(BTDevice);
            btListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnect(BluetoothDevice BTDevice) {
        activeSocket = btUtil.getActiveSocket();
    }

    @Override
    public void onAccelerationChanged(float x, float y, float z) {

    }

    @Override
    public void onShake(float force) {
        Log.d(TAG,"sensorLocked: "+sensorLocked);
        Log.d(TAG,"canSendMessage: "+canSendMessage);
        if (canSendMessage && sensorLocked){
            // Called when Shake Detected and Device Position has been locked/determined

            if (toBeControlledDevice!=null){
                if (toBeControlledDevice.getDeviceKind() == DeviceModel.DeviceKind.TOGGLE){
                    //Toast.makeText(this,"Toggling device "+toBeControlledDevice.getDeviceName()+", degree: "+toBeControlledDevice.getDeviceDegree(), Toast.LENGTH_SHORT).show();
                    sendMessageToBluetooth("T" + new DecimalFormat("00").format(Integer.parseInt(toBeControlledDevice.getDevicePin())));
                }else{
                    //parameterized
                    float refDegree = currentDegree;//storedDegree[1]
                    float tolerance = 60;
                    float lockedDegree = storedDegree[0];
                    float shakeDegree = refDegree;
                    float diffDegree = currentDegree - lockedDegree;
                    if (Math.abs(diffDegree) <= tolerance){
                        Log.d(TAG,"parameter device shall respond ");
                        boolean decrease = false;
                        boolean unsafe = lockedDegree<tolerance;
                        if (unsafe){
                            Log.d(TAG,"parameter device on unsafe case ");
                            //special case since the left will be higher than pin pointed position
                            if (shakeDegree>lockedDegree)
                                decrease = true;//shake on left
                        }else{
                            Log.d(TAG,"parameter device on safe case ");
                            if (shakeDegree < lockedDegree)
                                decrease = true;//shake on right
                        }
                        int currentValue = SharedPrefsUtils.getIntegerPreference(this,"p_"+toBeControlledDevice.getDevicePin(),0);

                        if (!decrease)
                            currentValue += PARAMETER_INCREMENT;
                        else {
                            if (currentValue == 99)
                                currentValue = 100;
                            currentValue -= PARAMETER_INCREMENT;
                        }

                        if (currentValue>99)
                            currentValue = 99;
                        if (currentValue < 0)
                            currentValue = 0;

                        sendMessageToBluetooth("P"
                                + new DecimalFormat("00").format(Integer.parseInt(toBeControlledDevice.getDevicePin()))
                                + new DecimalFormat("00").format(currentValue));

                        SharedPrefsUtils.setIntegerPreference(this,"p_"+toBeControlledDevice.getDevicePin(),currentValue);
                    }



                }
            }
//            turnOn = !turnOn;
//            String message = turnOn ? "daya/" : "mati/";
//            sendMessageToBluetooth(message);
        }
    }

    @Override
    public void onCompassRotate(float azimuth, float pitch, float roll) {

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
        currentDegree = degree;
        if (sensorLocked){
            storedDegree[1] = currentDegree;
        }
    }

    private void sendMessageToBluetooth(String message){
        if (activeSocket!=null){
            try {
                Log.d(TAG, "Sending message: " + message);
                OutputStream outputStream = activeSocket.getOutputStream();
                outputStream.write(message.getBytes());
                //sensorLocked = false;
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
