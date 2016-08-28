package com.threeosix.exp.shakemotiondetection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.threeosix.exp.shakemotiondetection.R;
import com.threeosix.exp.shakemotiondetection.adapters.DeviceListAdapter;
import com.threeosix.exp.shakemotiondetection.helper.DeviceDataManager;
import com.threeosix.exp.shakemotiondetection.interfaces.ControlDeviceListener;
import com.threeosix.exp.shakemotiondetection.models.DeviceModel;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by taufik on 8/27/16.
 */
public class DeviceListActivity extends AppCompatActivity {
    public static final int DEVICE_SETUP_CODE = 307;
    private static final int DEVICE_LIMIT = 99;
    @BindView(R.id.text_message)TextView tvMessageNoDevice;
    @BindView(R.id.list_view_device)ListView lvDeviceList;
    @BindView(R.id.button_add_new)Button btnAddNeWDevice;
    private DeviceDataManager deviceManager;
    private ArrayList<DeviceModel> devices;
    private DeviceListAdapter deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        ButterKnife.bind(this);
        deviceManager = new DeviceDataManager(this);
        initialize();
    }

    private void initialize(){
        devices = deviceManager.getAllDevices();
        tvMessageNoDevice.setVisibility(devices.size() == 0 ? View.VISIBLE:View.GONE);
        deviceAdapter = new DeviceListAdapter(this, devices);
        deviceAdapter.setCallback(new ControlDeviceListener() {
            @Override
            public void removeControlledDevice(DeviceModel device) {
                deviceManager.removeDevice(device);
                devices.remove(device);
                deviceAdapter.notifyDataSetChanged();
            }
        });
        lvDeviceList.setAdapter(deviceAdapter);
        btnAddNeWDevice.setVisibility(devices.size() < DEVICE_LIMIT ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_SETUP_CODE){
            if (resultCode == RESULT_OK){
                initialize();
            }
        }
    }

    @OnClick(R.id.button_add_new)
    public void addNewDevice(){
        Intent setupDeviceIntent = new Intent(this, DeviceSetupActivity.class);
        startActivityForResult(setupDeviceIntent, DEVICE_SETUP_CODE);
    }
}
