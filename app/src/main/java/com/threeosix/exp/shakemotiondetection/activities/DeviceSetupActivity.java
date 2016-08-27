package com.threeosix.exp.shakemotiondetection.activities;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.helper.AccelerometerManager;
import com.threeosix.exp.shakemotiondetection.helper.CompassManager;
import com.threeosix.exp.shakemotiondetection.helper.DeviceDataManager;
import com.threeosix.exp.shakemotiondetection.interfaces.SensorListener;
import com.threeosix.exp.shakemotiondetection.models.DeviceModel;
import com.threeosix.exp.shakemotiondetection.utils.SharedPrefsUtils;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceSetupActivity extends AppCompatActivity implements SensorListener {
    private final static String TAG = "DeviceSetupActivity";
    @BindView(R.id.text_degree_info)TextView tvDegreeInfo;
    @BindView(R.id.progress_loading)ProgressBar progressBar;
    private float currentDegree;
    private float lockedDegree;
    private DeviceDataManager deviceManager;
    private final long CALIBRATION_DELAY = 3000; // millisecond

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_setup);
        ButterKnife.bind(this);
        initialize();
        /**
         * Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
         .setAction("Action", null).show();
         */
    }

    private void initialize(){
        currentDegree = 0.f;
        deviceManager = new DeviceDataManager(this);
    }

    private void showCalibratingDialog(){
        boolean positionCalibrated = SharedPrefsUtils.getBooleanPreference(this, "calibrating_done", false);
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
                    SharedPrefsUtils.setBooleanPreference(DeviceSetupActivity.this, "calibrating_done", true);
                }
            }, CALIBRATION_DELAY);
        }


    }

    @Override
    public void onResume() {
        super.onResume();

        //Check device supported Compass sensor or not
        if (CompassManager.isSupported(this)) {

            //Start Compass Listening
            CompassManager.startListening(this);
            showCalibratingDialog();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

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

        if (CompassManager.isListening()) {

            //Stop Compass Listening
            CompassManager.stopListening();

//            Toast.makeText(getBaseContext(), "onDestroy Compass Stoped",
//                    Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.button_setup_device)
    public void setupDevice(){
//        progressBar.setVisibility(View.VISIBLE);
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                lockedDegree = currentDegree;
//                progressBar.setVisibility(View.INVISIBLE);
//                showDevicePopupDialog();
//            }
//        }, 1000);
        lockedDegree = currentDegree;
        showDevicePopupDialog();
    }

    private void showDevicePopupDialog(){
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.popup_setup_device, null);
        builderSingle.setView(convertView);
        final AlertDialog shownDialog = builderSingle.create();
        shownDialog.setCanceledOnTouchOutside(false);
        final EditText etPinNo = (EditText)convertView.findViewById(R.id.text_device_pin);
        final EditText etDeviceName = (EditText)convertView.findViewById(R.id.text_device_name);
        final RadioGroup rgDeviceKind = (RadioGroup)convertView.findViewById(R.id.radio_device_kind);
        final Button btnRadioDeviceSubmit = (Button)convertView.findViewById(R.id.button_submit_device_setup);
        btnRadioDeviceSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String devicePin = etPinNo.getText().toString();
                String deviceName = etDeviceName.getText().toString();
                if (devicePin.length() > 0 && deviceName.length() > 0) {
                    DeviceModel device = new DeviceModel(devicePin, deviceName, lockedDegree);
                    int selectedRadioBtnId = rgDeviceKind.getCheckedRadioButtonId();
                    if (selectedRadioBtnId == R.id.radio_motion)
                        device.setDeviceKind(DeviceModel.DeviceKind.MOTION);
                    shownDialog.dismiss();
                    saveDevice(device);
                } else {
                    Toast.makeText(DeviceSetupActivity.this, "Please provide correct device pin and name", Toast.LENGTH_SHORT).show();
                }


            }
        });

        shownDialog.show();
    }

    private void saveDevice(DeviceModel deviceModel){
        deviceManager.storeDevice(deviceModel);
        setResult(RESULT_OK, null);
        finish();
    }

    private void printPosition(){
        tvDegreeInfo.setText("position(degree): "+currentDegree+(char) 0x00B0);
    }

    @Override
    public void onAccelerationChanged(float x, float y, float z) {

    }

    @Override
    public void onShake(float force) {

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
        printPosition();
    }
}
