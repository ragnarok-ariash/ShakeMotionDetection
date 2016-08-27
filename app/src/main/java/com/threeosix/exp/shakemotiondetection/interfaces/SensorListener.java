package com.threeosix.exp.shakemotiondetection.interfaces;

import com.threeosix.exp.shakemotiondetection.helper.CompassManager;

/**
 * Created by taufik on 8/26/16.
 */
public interface SensorListener {
    public void onAccelerationChanged(float x, float y, float z);
    public void onShake(float force);
    public void onCompassRotate(float azimuth, float pitch, float roll);
    public void onCompassRotate(CompassManager.CompassDirection compassDirection, float degree);
}
