package com.example.mateus.testcastmdtw.utils;

import android.graphics.Color;
import android.os.Handler;
import android.widget.ImageButton;

import org.json.JSONArray;

import java.util.List;

/**
 * Created by mateus on 12/22/16.
 */

public class Utils {

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
        stdDeviations[1] = Math.sqrt(diffMeanY/(sensorDataSize - 1));
        stdDeviations[2] = Math.sqrt(diffMeanZ/(sensorDataSize - 1));

        return stdDeviations;
    }

    // Passes the data in allAccelData to a JSONArray.
    public static JSONArray convertToJSONArray(List<float[]> allAccelData) {

        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < allAccelData.size(); i++) {
            JSONArray jsonArrayItem = new JSONArray();
            jsonArrayItem.put(String.valueOf(allAccelData.get(i)[0]));
            jsonArrayItem.put(String.valueOf(allAccelData.get(i)[1]));
            jsonArrayItem.put(String.valueOf(allAccelData.get(i)[2]));

            jsonArray.put(jsonArrayItem);
        }

        return jsonArray;

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

}
