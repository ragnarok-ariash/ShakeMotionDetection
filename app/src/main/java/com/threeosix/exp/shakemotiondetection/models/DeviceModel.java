package com.threeosix.exp.shakemotiondetection.models;

/**
 * Created by taufik on 8/27/16.
 */
public class DeviceModel {
    public enum DeviceKind{
        TOGGLE,
        MOTION
    }
    private String devicePin;
    private String deviceName;
    private float deviceDegree;
    private int deviceKind;

    public DeviceModel(String devicePin, String deviceName, float deviceDegree) {
        this(devicePin, deviceName, deviceDegree, DeviceKind.TOGGLE);//toggle is default
    }

    public DeviceModel(String devicePin, String deviceName, float deviceDegree, DeviceKind deviceKind) {
        this.devicePin = devicePin;
        this.deviceName = deviceName;
        this.deviceDegree = deviceDegree;
        setDeviceKind(deviceKind);
    }

    public String getDevicePin() {
        return devicePin;
    }

    public void setDevicePin(String devicePin) {
        this.devicePin = devicePin;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public float getDeviceDegree() {
        return deviceDegree;
    }

    public void setDeviceDegree(float deviceDegree) {
        this.deviceDegree = deviceDegree;
    }

    public DeviceKind getDeviceKind() {
        switch (deviceKind){
            case 0:
                return DeviceKind.TOGGLE;
            case 1:
                return DeviceKind.MOTION;
        }
        return DeviceKind.TOGGLE;
    }

    public void setDeviceKind(DeviceKind deviceKind) {
        int deviceKindint = 0;
        switch (deviceKind){
            case TOGGLE:
                deviceKindint = 0;
                break;
            case MOTION:
                deviceKindint = 1;
                break;
        }
        this.deviceKind = deviceKindint;
    }
}
