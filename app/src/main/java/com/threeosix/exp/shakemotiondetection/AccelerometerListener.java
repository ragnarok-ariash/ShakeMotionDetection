package com.threeosix.exp.shakemotiondetection;

/**
 * Created by taufik on 8/26/16.
 */
public interface AccelerometerListener {
    public void onAccelerationChanged(float x, float y, float z);
    public void onShake(float force);
}
