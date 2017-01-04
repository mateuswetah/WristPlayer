package com.example.mateus.testcastmdtw.TiltMonitor;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateus on 12/28/16.
 */

public class TiltMonitor implements SensorEventListener,
                                    Application.ActivityLifecycleCallbacks{

    // Sensors
    private Sensor sensorAccel;
    private SensorManager sensorManagerAccel;

    // Tilt detection
    private boolean tiltBegin = false;
    private boolean tiltPeek = false;
    private long startTime = 0;
    private Double refXAngle = 0.0;

    // Activity associated
    private Activity activity;
    private boolean onlyActivity;

    // Singleton instance object
    private static TiltMonitor tiltMonitor;

    // A list of Observers (Listeners) Interface, to be implemented in the Activity
    private List<TiltMonitorListener> tiltMonitorListeners = new ArrayList<>();

    // Singleton pattern with private constructor and getInstance method
    private TiltMonitor() { super(); }

    public static TiltMonitor getInstance() {

        if(tiltMonitor == null) {
            tiltMonitor = new TiltMonitor();
        }

        return tiltMonitor;
    }

    // Set activity step, required to register listeners and sensorManager
    public void setActivity(Activity activity) {
        setActivity(activity, false);
    }

    private void setActivity(Activity activity, boolean onlyActivity) {

        if (this.activity != null) {
            if (sensorManagerAccel != null) {
                sensorManagerAccel.unregisterListener(this);
                this.activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            }
        }

        this.onlyActivity = onlyActivity;
        this.activity     = activity;
        this.activity.getApplication().registerActivityLifecycleCallbacks(this);

        sensorManagerAccel = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        sensorAccel  = sensorManagerAccel.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }


    // SensorEventListener methods implementation
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.d("SENSOR", "STATUS UNRELIABLE");
            return;
        }

        float[] oneAccelData = new float[3];
        oneAccelData[0] = event.values[0] / SensorManager.GRAVITY_EARTH;
        oneAccelData[1] = event.values[1] / SensorManager.GRAVITY_EARTH;
        oneAccelData[2] = event.values[2] / SensorManager.GRAVITY_EARTH;

        verifyTiltMovement(oneAccelData);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("SENSOR", "ACCURACY: " + accuracy);
    }

    // Analyses the current accelerometer data to see if a Tilt was made
    private void verifyTiltMovement(float[] accelData) {

        long now = System.currentTimeMillis();
        Double angleX, angleY, angleZ;

        Double measuredGp = Math.sqrt(Math.pow(accelData[0],2) + Math.pow(accelData[1],2) + Math.pow(accelData[2],2));

        angleX = Math.toDegrees(Math.acos(accelData[0]/measuredGp));
        angleY = Math.toDegrees(Math.acos(accelData[1]/measuredGp));
        angleZ = Math.toDegrees(Math.acos(accelData[2]/measuredGp));

        // Beginning of a Tilt rotation
        if (angleY > 100 && angleZ > 10 && !tiltBegin) {
            tiltBegin = true;
            startTime = System.currentTimeMillis();
            refXAngle = angleX;
        }

        // Beginning of the Peek of the Tilt rotation
        if (angleY > 170 && angleZ > 80) {
            tiltPeek = true;

            // Cancels analysis in case there was a significant X axis variation
            if (Math.abs(refXAngle - angleX) > 45) {
                tiltPeek = false;
                tiltBegin = false;
                startTime = 0;

                Log.d("ANGLE", "X AXIS CHANGED! DIFF: " + Math.abs(refXAngle - angleX)  );
            }
        }

        // Returning from a Peek of the Tilt rotation
        if (angleY < 100 && angleZ < 10) {

            // Tilt confirmed!
            if (tiltBegin && tiltPeek)
                notifyTiltMonitorListeners();

            tiltPeek = false;
            tiltBegin = false;
        }

        // Cancels analysis in case it took more than 1.5secs
        if (now - startTime > 1500) {
            tiltPeek = false;
            tiltBegin = false;
            startTime = 0;
        }

    }

    // Observer pattern methods --------------------------------------------------------------------
    public void registerTiltMonitorListener(TiltMonitorListener tiltMonitorListener) {
        this.tiltMonitorListeners.add(tiltMonitorListener);
    }

    public void unregisterTiltMonitorListener(TiltMonitorListener tiltMonitorListener) {
        this.tiltMonitorListeners.remove(tiltMonitorListener);
    }

    public void notifyTiltMonitorListeners() {

        // Notifies each observer in a new thread to avoid keeping the main one busy
        for (final TiltMonitorListener tiltMonitorListener:tiltMonitorListeners) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    tiltMonitorListener.onTiltDetected();
                }
            };
            new Thread(runnable).start();
        }
    }

    //A ActivityLifeCycles Callbacks. Register and unregister the sensor listener
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {

        if (sensorManagerAccel != null) {
            if (!onlyActivity || activity == this.activity) {
                sensorManagerAccel.registerListener(this, sensorAccel, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (sensorManagerAccel != null) {
            if (!onlyActivity || activity == this.activity) {
                sensorManagerAccel.unregisterListener(this);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (!onlyActivity || activity == this.activity) {
            this.tiltMonitorListeners.remove(activity);
        }
    }
}
