package com.example.mateus.testcastmdtw.TimedGestureCollectorr;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.example.mateus.testcastmdtw.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateus on 12/28/16.
 */

public class TimedGestureCollector implements SensorEventListener,
                                    Application.ActivityLifecycleCallbacks{

    // Sensors
    private Sensor sensorAccel;
    private SensorManager sensorManagerAccel;

    // Time Series Setting
    private long startTime = 0;
    private long timeSeriesDurationMillis = 1200;
    private boolean collectingData = false;

    // List of Sensor data to be returned
    private List<float[]> sensorDataCollected = new ArrayList<>();

    // Activity associated
    private Activity activity;
    private boolean onlyActivity;

    // Singleton instance object
    private static TimedGestureCollector timedGestureCollector;

    // A list of Observers (Listeners) Interface, to be implemented in the Activity
    private List<TimedGestureCollectorListener> timedGestureCollectorListeners = new ArrayList<>();

    // Singleton pattern with private constructor and getInstance method
    private TimedGestureCollector() { super(); }

    public static TimedGestureCollector getInstance() {

        if(timedGestureCollector == null) {
            timedGestureCollector = new TimedGestureCollector();
        }

        return timedGestureCollector;
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
        //sensorManagerAccel.registerListener(this, sensorAccel, SensorManager.SENSOR_DELAY_GAME);
    }

    // SensorEventListener methods implementation
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.d("SENSOR", "STATUS UNRELIABLE");
            return;
        }

        float[] oneAccelData = new float[3];
        oneAccelData[0] = event.values[0];
        oneAccelData[1] = event.values[1];
        oneAccelData[2] = event.values[2];

        sensorDataCollected.add(oneAccelData);

        if (collectingData == true) {
            long now = System.currentTimeMillis();
            if (now - startTime > timeSeriesDurationMillis) {
                this.notifyTimeSeriesCollectorListeners(sensorDataCollected);
                collectingData = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("SENSOR", "ACCURACY: " + accuracy);
    }

    // Timer series setting methods
    public void startCollecting() {
        sensorDataCollected = new ArrayList<>();
        collectingData = true;
        startTime = System.currentTimeMillis();
    }

    public void setTimeSeriesDurationMillis (long timeSeriesDurationMillis) {
        this.timeSeriesDurationMillis = timeSeriesDurationMillis;
    }

    // Checks the standard deviation of the movement to see if the wrist is or not idle
    private boolean checkIfIdleState(List<float[]> sensorGestureData) {

        Double[] stdDeviations = Utils.getStandardDeviation(sensorGestureData);

        //Log.d("DEVIATIONS", stdDeviations[0] + ", " + stdDeviations[1] + ", " + stdDeviations[2]);

        if (stdDeviations[0] < 1.0 && stdDeviations[1] < 1.0 && stdDeviations[2] < 1.0)
            return true;
        else
            return false;
    }

    // Observer pattern methods --------------------------------------------------------------------
    public void registerTimeSeriesCollectorListener(TimedGestureCollectorListener timedGestureCollectorListener) {
        this.timedGestureCollectorListeners.add(timedGestureCollectorListener);
    }

    public void unregisterTimeSeriesCollectorListener(TimedGestureCollectorListener timedGestureCollectorListener) {
        this.timedGestureCollectorListeners.remove(timedGestureCollectorListener);
    }

    public void notifyTimeSeriesCollectorListeners(final List<float[]> sensorDataCollected) {

        // Checks if the gesture is Idle first
        if (checkIfIdleState(sensorDataCollected)) {

            // Notifies each observer in a new thread to avoid keeping the main one busy
            for (final TimedGestureCollectorListener timedGestureCollectorListener : timedGestureCollectorListeners) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        timedGestureCollectorListener.onIdleStateDetected();
                    }
                };
                new Thread(runnable).start();
            }
        } else {

            // Notifies each observer in a new thread to avoid keeping the main one busy
            for (final TimedGestureCollectorListener timedGestureCollectorListener : timedGestureCollectorListeners) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        timedGestureCollectorListener.onSensorTimeSeriesCollected(sensorDataCollected);
                    }
                };
                new Thread(runnable).start();
            }
        }

    }


    //ActivityLifeCycles Callbacks. Register and unregister the sensor listener
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
            this.timedGestureCollectorListeners.remove(activity);
        }
    }
}
