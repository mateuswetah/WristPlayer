package com.example.mateus.testcastmdtw.triggerdetection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mateus on 12/16/16.
 */

public class SensorTriggerData implements Serializable {

    public List<float[]> Accel;

    public SensorTriggerData(List<float[]> formattedData) {
        this.Accel = new ArrayList<>();
        this.Accel.addAll(formattedData);
    }

    public SensorTriggerData(List<float[]> formattedData, int offset) {

        this.Accel = new ArrayList<>();

        for (int i = offset - 1; i >= 0; i--) {
            this.Accel.add(formattedData.remove(i));
        }
    }

    public SensorTriggerData(){
        this.Accel = new ArrayList<>();
    }

    public void updateData(List<float[]> formattedData, int offset) {

        this.Accel.clear();

        for (int i = offset - 1; i >= 0; i--) {
            this.Accel.add(formattedData.remove(i));
        }

        return ;
    }


    public List<Double> getXData(){

        List<Double> XData = new ArrayList<Double>();

        for (int i = 0; i < Accel.size(); i++) {
            XData.add(i, Double.valueOf(Accel.get(i)[0]));
        }
        return XData;
    }
    public List<Double> getYData(){

        List<Double> YData = new ArrayList<Double>();

        for (int i = 0; i < Accel.size(); i++) {
            YData.add(i, Double.valueOf(Accel.get(i)[1]));
        }
        return YData;
    }
    public List<Double> getZData(){

        List<Double> ZData = new ArrayList<Double>();

        for (int i = 0; i < Accel.size(); i++) {
            ZData.add(i, Double.valueOf(Accel.get(i)[2]));
        }
        return ZData;
    }

    public int getDataSize() {
        if (Accel != null)
            return this.Accel.size();
        else
            return 0;
    }

}

