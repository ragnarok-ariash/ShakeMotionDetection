package com.threeosix.exp.shakemotiondetection.helper;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.threeosix.exp.shakemotiondetection.interfaces.SensorListener;

import java.util.List;

/**
 * Created by taufik on 8/27/16.
 */
public class CompassManager {
    private static Context aContext=null;


    /** Accuracy configuration */
    private static float threshold  = 15.0f;
    private static int interval     = 200;
    static final float ALPHA = 0.25f;

    private static Sensor sensor;
    private static SensorManager sensorManager;
    // you could use an OrientationListener array instead
    // if you plans to use more than one listener
    private static SensorListener listener;

    /** indicates whether or not Accelerometer Sensor is supported */
    private static Boolean supported;
    /** indicates whether or not Accelerometer Sensor is running */
    private static boolean running = false;

    public enum CompassDirection {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        UNKNOWN
    }

    /**
     * Returns true if the manager is listening to orientation changes
     */
    public static boolean isListening() {
        return running;
    }

    /**
     * Unregisters listeners
     */
    public static void stopListening() {
        running = false;
        try {
            if (sensorManager != null && sensorEventListener != null) {
                sensorManager.unregisterListener(sensorEventListener);
            }
        } catch (Exception e) {}
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    public static boolean isSupported(Context context) {
        aContext = context;
        if (supported == null) {
            if (aContext != null) {


                sensorManager = (SensorManager) aContext.
                        getSystemService(Context.SENSOR_SERVICE);

                // Get all sensors in device
                List<Sensor> sensors = sensorManager.getSensorList(
                        Sensor.TYPE_MAGNETIC_FIELD);

                supported = new Boolean(sensors.size() > 0);



            } else {
                supported = Boolean.FALSE;
            }
        }
        return supported;
    }

    /**
     * Configure the listener for shaking
     * @param threshold
     *             minimum acceleration variation for considering shaking
     * @param interval
     *             minimum interval between to shake events
     */
    public static void configure(int threshold, int interval) {
        CompassManager.threshold = threshold;
        CompassManager.interval = interval;
    }

    /**
     * Registers a listener and start listening
     * @param sensorListener
     *             callback for accelerometer events
     */
    public static void startListening( SensorListener sensorListener)
    {

        sensorManager = (SensorManager) aContext.
                getSystemService(Context.SENSOR_SERVICE);

        // Take all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_ORIENTATION);
//        List<Sensor> mSensors = sensorManager.getSensorList(
//                Sensor.TYPE_ACCELEROMETER);

        if (sensors.size() > 0) {

            sensor = sensors.get(0);

            // Register Accelerometer Listener
            running = sensorManager.registerListener(
                    sensorEventListener, sensor,
                    SensorManager.SENSOR_DELAY_GAME);

            listener = sensorListener;
        }

//        if (mSensors.size() > 0) {
//
//            // Register Accelerometer Listener
//            running = sensorManager.registerListener(
//                    sensorEventListener, mSensors.get(0),
//                    SensorManager.SENSOR_DELAY_GAME);
//        }


    }

    /**
     * Configures threshold and interval
     * And registers a listener and start listening
     * @param sensorListener
     *             callback for accelerometer events
     * @param threshold
     *             minimum acceleration variation for considering shaking
     * @param interval
     *             minimum interval between to shake events
     */
    public static void startListening(
            SensorListener sensorListener,
            int threshold, int interval) {
        configure(threshold, interval);
        startListening(sensorListener);
    }

    /**
     * Low pass filter to eliminate fluctuations
     */
    protected static float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * The listener that listen to events from the accelerometer listener
     */
    private static SensorEventListener sensorEventListener =
            new SensorEventListener() {

                private long now = 0;
                private long timeDiff = 0;
                private long lastUpdate = 0;
//                private long lastShake = 0;

                private float azimuth = 0;
                private float pitch = 0;
                private float roll = 0;
                private float lastAzimuth = 0;
                private float lastPitch = 0;
                private float lastRoll = 0;
//                private float force = 0;

                public void onAccuracyChanged(Sensor sensor, int accuracy) {}

                public void onSensorChanged(SensorEvent event) {
                    // get the angle around the z-axis rotated
                    float degree = Math.round(event.values[0]);

                    //tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");
                    CompassDirection direction = CompassDirection.NORTH;
                    if((degree >= 340 && degree <= 360) || degree >=0 && degree <= 20){
                        direction = CompassDirection.NORTH;
                    }else if(degree >= 70 && degree <= 110){
                        direction = CompassDirection.EAST;
                    }else if(degree >=160 && degree <= 200){
                        direction = CompassDirection.SOUTH;
                    }else if(degree >= 240 && degree <= 290){
                        direction = CompassDirection.WEST;
                    }else{
                        direction = CompassDirection.UNKNOWN;
                    }
                    listener.onCompassRotate(direction, degree);

                    // use the event timestamp as reference
                    // so the manager precision won't depends
                    // on the SensorListener implementation
                    // processing time
                    now = event.timestamp;
//                    float mGeomagnetic[] = new float[event.values.length];
//                    float mGravity[] = new float[event.values.length];
//                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//                        mGravity = lowPass(event.values.clone(), mGravity);
//                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//                        mGeomagnetic = lowPass(event.values.clone(), mGeomagnetic);
//
//                    if (mGravity != null && mGeomagnetic != null) {
//                        float R[] = new float[9];
//                        float I[] = new float[9];
//                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
//                        if (success) {
//                            float orientation[] = new float[3];
//                            SensorManager.getOrientation(R, orientation);
//                            azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
//                            pitch = orientation[1];
//                            roll = orientation[2];
//                            // trigger change event
//                            listener.onCompassRotate(azimuth, pitch, roll);
//                        }
//                    }

                }

            };
}
