package com.example.mateus.testcastmdtw.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ImageButton;

import com.example.mateus.testcastmdtw.triggerdetection.SensorTriggerData;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by mateus on 12/22/16.
 */

public class Utils {

    // Apply a low pass filter over a array of type SensorTriggerData
    public static void lowPassFilter(SensorTriggerData sensorTriggerData) {

        for (int i = 0; i < sensorTriggerData.Accel.size(); i++ ) {
            if (i == 0 ) {
                sensorTriggerData.Accel.get(i)[0] = (sensorTriggerData.Accel.get(i)[0] + sensorTriggerData.Accel.get(i+1)[0])/(float)3.0;
                sensorTriggerData.Accel.get(i)[1] = (sensorTriggerData.Accel.get(i)[1] + sensorTriggerData.Accel.get(i+1)[1])/(float)3.0;
                sensorTriggerData.Accel.get(i)[2] = (sensorTriggerData.Accel.get(i)[2] + sensorTriggerData.Accel.get(i+1)[2])/(float)3.0;
            } else if (i == sensorTriggerData.Accel.size() - 1){
                sensorTriggerData.Accel.get(i)[0] = (sensorTriggerData.Accel.get(i-1)[0] + sensorTriggerData.Accel.get(i)[0])/(float)3.0;
                sensorTriggerData.Accel.get(i)[1] = (sensorTriggerData.Accel.get(i-1)[1] + sensorTriggerData.Accel.get(i)[1])/(float)3.0;
                sensorTriggerData.Accel.get(i)[2] = (sensorTriggerData.Accel.get(i-1)[2] + sensorTriggerData.Accel.get(i)[2])/(float)3.0;
            } else {
                sensorTriggerData.Accel.get(i)[0] = (sensorTriggerData.Accel.get(i-1)[0] + sensorTriggerData.Accel.get(i)[0] + sensorTriggerData.Accel.get(i+1)[0])/(float)3.0;
                sensorTriggerData.Accel.get(i)[1] = (sensorTriggerData.Accel.get(i-1)[1] + sensorTriggerData.Accel.get(i)[1] + sensorTriggerData.Accel.get(i+1)[1])/(float)3.0;
                sensorTriggerData.Accel.get(i)[2] = (sensorTriggerData.Accel.get(i-1)[2] + sensorTriggerData.Accel.get(i)[2] + sensorTriggerData.Accel.get(i+1)[2])/(float)3.0;
            }
        }
    }

    // Obtain separately the standard deviation of the axis X,Y and Z in a SensorGestureData
    public static Double[] getStandardDeviation(List<float[]> sensorGestureData){

        Double meanX = 0.0;
        Double meanY = 0.0;
        Double meanZ = 0.0;

        int sensorDataSize = sensorGestureData.size();

        Double diffMeanX = 0.0;
        Double diffMeanY = 0.0;
        Double diffMeanZ = 0.0;

        Double[] stdDeviations = new Double[3];

        for (int i = 0; i < sensorDataSize; i++) {
            meanX += sensorGestureData.get(i)[0];
            meanY += sensorGestureData.get(i)[1];
            meanZ += sensorGestureData.get(i)[2];
        }

        meanX = meanX/sensorDataSize;
        meanY = meanY/sensorDataSize;
        meanZ = meanZ/sensorDataSize;

        for (int i = 0; i < sensorDataSize; i++) {
            diffMeanX += Math.pow(sensorGestureData.get(i)[0] - meanX, 2);
            diffMeanY += Math.pow(sensorGestureData.get(i)[1] - meanY, 2);
            diffMeanZ += Math.pow(sensorGestureData.get(i)[2] - meanZ, 2);
        }

        stdDeviations[0] = Math.sqrt(diffMeanX/(sensorDataSize - 1));
        stdDeviations[1] = Math.sqrt(diffMeanX/(sensorDataSize - 1));
        stdDeviations[2] = Math.sqrt(diffMeanX/(sensorDataSize - 1));

        return stdDeviations;
    }

    // Obtain separately the standard deviation of the axis X,Y and Z in a SensorTriggerData
    public static Double[] getStandardDeviation(SensorTriggerData sensorTriggerData){

        Double meanX = 0.0;
        Double meanY = 0.0;
        Double meanZ = 0.0;

        int sensorDataSize = sensorTriggerData.getDataSize();

        Double diffMeanX = 0.0;
        Double diffMeanY = 0.0;
        Double diffMeanZ = 0.0;

        Double[] stdDeviations = new Double[3];

        for (int i = 0; i < sensorDataSize; i++) {
            meanX += sensorTriggerData.getXData().get(i);
            meanY += sensorTriggerData.getYData().get(i);
            meanZ += sensorTriggerData.getZData().get(i);
        }

        meanX = meanX/sensorDataSize;
        meanY = meanY/sensorDataSize;
        meanZ = meanZ/sensorDataSize;

        //Log.d("MEANS", meanX + ", " + meanY + ", " + meanZ);

        for (int i = 0; i < sensorDataSize; i++) {
            diffMeanX += Math.pow(sensorTriggerData.getXData().get(i) - meanX, 2);
            diffMeanY += Math.pow(sensorTriggerData.getYData().get(i) - meanY, 2);
            diffMeanZ += Math.pow(sensorTriggerData.getZData().get(i) - meanZ, 2);
        }

        stdDeviations[0] = Math.sqrt(diffMeanX/(sensorDataSize - 1));
        stdDeviations[1] = Math.sqrt(diffMeanX/(sensorDataSize - 1));
        stdDeviations[2] = Math.sqrt(diffMeanX/(sensorDataSize - 1));

        return stdDeviations;
    }

    // Passes the data in AllGestureAccel to a JSONArray.
    public static void prepareJSONArray(List<float[]> allGestureAccel, JSONArray jsonAllAccel) {

        for (int i = 0; i < allGestureAccel.size(); i++) {
            JSONArray jsonAccel = new JSONArray();
            jsonAccel.put(String.valueOf(allGestureAccel.get(i)[0]));
            jsonAccel.put(String.valueOf(allGestureAccel.get(i)[0]));
            jsonAccel.put(String.valueOf(allGestureAccel.get(i)[0]));

            jsonAllAccel.put(jsonAccel);
        }

    }

    // Auxiliary function to create the effect of selected button
    public static void darkenButtonBackground (final ImageButton imageButton) {
        // Change image
        imageButton.setBackgroundColor(Color.argb(100,25,5,0));
        // Handler
        new Handler().postDelayed(new Runnable() {

            public void run() {
                // Revert back to original image
                imageButton.setBackgroundColor(Color.argb(0,0,0,0));
            }
        }, 3000L);    // 5000 milliseconds(5 seconds) delay
    }

    // Calls the default painel to request permission on writing files
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Log.d("ACTIVITY", "NO PERMISSION GRANTED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}
