package com.example.mateus.testcastmdtw.TimedGestureCollectorr;

import java.util.List;

/**
 * Created by mateus on 12/28/16.
 */

public interface TimedGestureCollectorListener {

    void onSensorTimeSeriesCollected(List<float[]> sensorDataCollected);
    void onIdleStateDetected();

}
