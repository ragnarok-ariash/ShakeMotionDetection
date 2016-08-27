package com.threeosix.exp.shakemotiondetection.helper;

import android.content.Context;

import com.google.gson.Gson;
import com.threeosix.exp.shakemotiondetection.models.DeviceModel;
import com.threeosix.exp.shakemotiondetection.utils.SharedPrefsUtils;

import java.util.ArrayList;

/**
 * Created by taufik on 8/27/16.
 */
public class DeviceDataManager {
    public final static String PIN_KEY = "pin_key";

    private Context ctx;

    public DeviceDataManager(Context ctx) {
        this.ctx = ctx;
    }

    public ArrayList<DeviceModel> getAllDevices(){
        ArrayList<DeviceModel> devices = new ArrayList<>();
        ArrayList<String> devicePins = SharedPrefsUtils.getStringArrayPreference(ctx, PIN_KEY);
        if (devicePins.size()>0){
            for (String devicePin : devicePins){
                devices.add(getDeviceByPin(devicePin));
            }
        }
        return devices;
    }

    public void storeDevice(DeviceModel device){
        ArrayList<String> devicePins = SharedPrefsUtils.getStringArrayPreference(ctx, PIN_KEY);
        Gson gson = new Gson();
        String deviceData = gson.toJson(device);
        //store by pin
        SharedPrefsUtils.setStringPreference(ctx, device.getDevicePin(),deviceData);
        //store pin keys
        devicePins.add(device.getDevicePin());
        SharedPrefsUtils.setStringArrayPreference(ctx, PIN_KEY, devicePins);
    }

    public void removeDevice(DeviceModel device){
        ArrayList<String> devicePins = SharedPrefsUtils.getStringArrayPreference(ctx, PIN_KEY);
        Gson gson = new Gson();
        //remove by pin
        SharedPrefsUtils.removeStringPreference(ctx, device.getDevicePin());
        //remove from pin keys
        devicePins.remove(device.getDevicePin());
        SharedPrefsUtils.setStringArrayPreference(ctx, PIN_KEY, devicePins);
    }

    public DeviceModel getDeviceByPin(String devicePin){
        String deviceData = SharedPrefsUtils.getStringPreference(ctx, devicePin);
        Gson gson = new Gson();
        DeviceModel device = gson.fromJson(deviceData, DeviceModel.class);
        return device;
    }
}
