package com.example.mateus.testcastmdtw;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.example.mateus.testcastmdtw.triggerdetection.MDDTW;
import com.example.mateus.testcastmdtw.triggerdetection.SensorTriggerData;
import com.example.mateus.testcastmdtw.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Sensors
    private SensorManager managerAccel, managerGyro;
    private Sensor sensorAccel, sensorGyro;
    private SensorEventListener AccelListener, GyroListener;

    // Sensor trigger data
    private List<float[]> allTriggerGyro, allGestureGyro, allTemplateGyro;
    private List<float[]> allTriggerAccel, allGestureAccel, allTemplateAccel;
    private SensorTriggerData sensorDataTemplate;
    private SensorTriggerData sensorTriggerData;

    // JSON parsing
    JSONObject jsonObject;

    // Timer used for trigger detection
    Timer timer;
    TimerTask timerTask;

    // Visual Components
    private ImageButton playPauseButton, volumeDownButton, volumeUpButton, nextButton, prevButton;
    private ToggleButton buttonRecord;
    boolean playButtonState = false;

    // Flags to control with template to record
    private boolean templateRecording = false;
    private boolean triggerTemplateRecording = false;

    // Related to Sending the Message
    private static final String WEARABLE_MAIN = "WearableMain";
    private Node mNode;
    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_PATH = "/from-wear";


    public static MainActivity controllerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        controllerActivity = this;

        // Prevents Screen getting off. The same flag keeps the sensor data collecting active.
        Window w = this.getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Verify permissions
        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
        int permissionCheck3 = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        Log.d("ACCESS_WIFI_STATE", "" + (permissionCheck1 == PackageManager.PERMISSION_GRANTED));
        Log.d("ACCESS_NETWORK_STATE", "" + (permissionCheck2 == PackageManager.PERMISSION_GRANTED));
        Log.d("ACCESS_INTERNET", "" + (permissionCheck3 == PackageManager.PERMISSION_GRANTED));

        // Extra permissions needed for reading template movement files
        Utils.verifyStoragePermissions(this);

        // Acquires wake lock, as the sensor data collecting should not stop.
        //acquireWakeLock();

        // Gets the googleApiClient for communicating with mobile pair
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        // Associate the XML items for displaying the values
        buttonRecord     = (ToggleButton) findViewById(R.id.ButtonRecord);
        playPauseButton  = (ImageButton)  findViewById(R.id.playPauseButton);
        volumeDownButton = (ImageButton)  findViewById(R.id.volumeDownButton);
        volumeUpButton   = (ImageButton)  findViewById(R.id.volumeUpButton);
        nextButton       = (ImageButton)  findViewById(R.id.nextButton);
        prevButton       = (ImageButton)  findViewById(R.id.prevButton);

        // Set the sensors
        managerAccel = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = managerAccel.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        managerAccel.registerListener(AccelListener, sensorAccel, SensorManager.SENSOR_DELAY_GAME);

        //managerGyro = (SensorManager) getSystemService(SENSOR_SERVICE);
        //sensorGyro = managerGyro.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Initialize the JSON object
        jsonObject = new JSONObject();

        // Initialize the trigger data arrays
        allTriggerAccel = new ArrayList<>();
        allGestureAccel = new ArrayList<>();
        allTemplateAccel = new ArrayList<>();
        //allTriggerGyro = new ArrayList<>();
        //allTemplateGyro = new ArrayList<>();
        sensorDataTemplate = new SensorTriggerData(readFromLocalTemplate());
        Utils.lowPassFilter(sensorDataTemplate);
        sensorTriggerData = new SensorTriggerData();

        // Setting timer for continuously comparing movements, every 500secs
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {

                if(!templateRecording && !triggerTemplateRecording && !buttonRecord.isChecked()) {

                    //int i = allTriggerAccel.size();
                    //sensorTriggerData.updateData(allTriggerAccel, i);
                    sensorTriggerData = new SensorTriggerData(allTriggerAccel);
                    allTriggerAccel.clear(); // Clear array for next comparison.

                    if (sensorTriggerData == null || sensorTriggerData.getDataSize() == 0) {
                        Log.d("ACTIVITY", "NO RECENTLY RECORDED DATA");
                    } else if (sensorTriggerData.getDataSize() < 45) {
                        Log.d("ACTIVITY", "NO ENOUGH DATA");
                    } else if (sensorDataTemplate == null || sensorDataTemplate.getDataSize() == 0) {
                        Log.d("ACTIVITY", "NO TEMPLATE RECORDED");
                    } else {
                        if (checkIfIdleState(sensorTriggerData) == true)
                            Log.d("ACTIVITY", "IDLE STATE ON TRIGGER DETECTION");
                        else
                            compareTriggerMovement();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, new Date(), 510);

        // Sensors Listeners
        AccelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                // If the ButtonRecord is pressed, either:
                // save data for gestures templates
                if (templateRecording) {
                    float[] oneGestureAccel = new float[3];
                    oneGestureAccel[0] = event.values[0];
                    oneGestureAccel[1] = event.values[1];
                    oneGestureAccel[2] = event.values[2];

                    allGestureAccel.add(oneGestureAccel);

                // or save data for trigger templates
                } else if (triggerTemplateRecording) {
                    float[] oneTemplateAccel = new float[3];
                    oneTemplateAccel[0] = event.values[0];
                    oneTemplateAccel[1] = event.values[1];
                    oneTemplateAccel[2] = event.values[2];

                    allTemplateAccel.add(oneTemplateAccel);
                } else {

                    // Continuous trigger data collecion
                    float[] oneAccel = new float[3];
                    oneAccel[0] = event.values[0];
                    oneAccel[1] = event.values[1];
                    oneAccel[2] = event.values[2];

                    allTriggerAccel.add(oneAccel);
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                Log.d("SENSOR_ACCURACY", "" + accuracy);
            }
        };

     /*   GyroListener = new SensorEventListener() {
   //         @Override
 //           public void onSensorChanged(SensorEvent event) {
//
//                if (ButtonRecord.isChecked()) {
//                    JSONArray jsonGyro = new JSONArray();
//
//                    jsonGyro.put(String.valueOf(event.values[0]));
//                    jsonGyro.put(String.valueOf(event.values[1]));
//                    jsonGyro.put(String.valueOf(event.values[2]));
//
//                    jsonAllGyro.put(jsonGyro);
//
//                }

//            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };*/

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                templateRecording = true;
                //buttonRecord.setChecked(true);

                try {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {

                        try {
                            //jsonObject = new JSONObject();
                            if (checkIfIdleState(allGestureAccel) == true) {

                                JSONArray jsonAllAccel = new JSONArray();
                                Utils.prepareJSONArray(allGestureAccel, jsonAllAccel);
                                jsonObject.put("Accel", jsonAllAccel);
                                //jsonObject.put("Gyro", jsonAllGyro);

                                sendMessage(jsonObject.toString(2));

                                // Reset the JSON objects
                                jsonObject = new JSONObject();

                            } else {
                                Log.d("ACTIVITY", "IDLE STATE ON GESTURE DETECTION");
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        templateRecording = false;
                        buttonRecord.setChecked(false);

                        }
                    }, 1250);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        buttonRecord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                triggerTemplateRecording = true;
                buttonRecord.setChecked(true);

                allTemplateAccel.clear();

                try {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            writeOnLocalTemplate();

                            triggerTemplateRecording = false;
                            buttonRecord.setChecked(false);


                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

                            long[] vibrationPattern = {0,200,0,200};
                            final int indexInPatternToRepeat = -1; // Don't repeat
                            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                        }
                    }, 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                return true;
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    jsonObject.put("button","PLAY");
                    Utils.darkenButtonBackground(playPauseButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    jsonObject.put("button","VOLUME_DOWN");
                    Utils.darkenButtonBackground(volumeDownButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    jsonObject.put("button","VOLUME_UP");
                    Utils.darkenButtonBackground(volumeUpButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    jsonObject.put("button","NEXT");
                    Utils.darkenButtonBackground(nextButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    jsonObject.put("button","PREV");
                    Utils.darkenButtonBackground(prevButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    // Checks the standard deviation of the movement to see if the wrist is or not idle
    private boolean checkIfIdleState(SensorTriggerData sensorTriggerData) {

        Double[] stdDeviations = Utils.getStandardDeviation(sensorTriggerData);

        if (stdDeviations[0] < 0.1 && stdDeviations[1] < 0.1 && stdDeviations[2] < 0.1)
            return true;
        else
            return false;
    }
    // Checks the standard deviation of the movement to see if the wrist is or not idle
    private boolean checkIfIdleState(List<float[]> sensorGestureData) {

        Double[] stdDeviations = Utils.getStandardDeviation(sensorGestureData);

        //Log.d("DEVIATIONS", stdDeviationX + ", " + stdDeviationY + ", " + stdDeviationZ);

        if (stdDeviations[0] < 1.0 && stdDeviations[1] < 1.0 && stdDeviations[2] < 1.0)
            return true;
        else
            return false;
    }


    //--------------------------------------------------------------------------------
    // TRIGGER DETECTION METHODS --------------------------------------------------------

    public void writeOnLocalTemplate() {

        FileOutputStream outStream = null;
        Log.d("ACTIVITY", "WRITE");
        try {
            outStream = new FileOutputStream(Environment.getExternalStorageDirectory()  + "/wrist_template.dat");
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeInt(allTemplateAccel.size()); // Save size first

            for(int i  = 0; i < allTemplateAccel.size(); i++) {
                objectOutStream.writeObject(allTemplateAccel.get(i));
            }

            objectOutStream.close();

            // Passes the new template data from file to object
            sensorDataTemplate = new SensorTriggerData(readFromLocalTemplate());
            Utils.lowPassFilter(sensorDataTemplate);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<float[]> readFromLocalTemplate() {

        FileInputStream inStream = null;

        try {
            inStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/wrist_template.dat");

            ObjectInputStream objectInStream = new ObjectInputStream(inStream);
            int count = objectInStream.readInt(); // Get the number of data
            List<float[]> sensorDataRaw = new ArrayList<>();

            for (int i=0; i < count; i++)
                sensorDataRaw.add((float[]) objectInStream.readObject());

            objectInStream.close();

            return sensorDataRaw;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void compareTriggerMovement() {

        Utils.lowPassFilter(sensorTriggerData);
        // Log.d("SENSOR_X", sensorData.getXData().toString());
        // Log.d("SENSOR_Y", sensorData.getYData().toString());
        // Log.d("SENSOR_Z", sensorData.getZData().toString());

        double smallestDist = 90.00;
        int smallestIndex = 6;

        //for (int i = 0; i < sensorDataTemplates.size(); i++) {
        if (sensorDataTemplate.getDataSize() == 0) {
            Log.d("ACTIVITY", "FAILED TO LOAD " + 0 + "TH TEMPLATE FILE");
        } else {
            double dist = new MDDTW(sensorDataTemplate, sensorTriggerData).getDistancia();
            Log.d("DISTANCIA", "" + dist);

            if (dist < smallestDist) {
                smallestDist = dist;
                smallestIndex = 1;
            }
        }
        //}

        if (smallestIndex == 1) {
            Log.d("TRIGGER", "TWISTED!");

            try {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

                        long[] vibrationPattern = {0,200,0,0};
                        final int indexInPatternToRepeat = -1; // Don't repeat
                        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                        buttonRecord.callOnClick();
                    }
                }, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    //--------------------------------------------------------------------------------
    // WEAR AND MOBILE COMMUNICATION METHODS -----------------------------------------
    private void sendMessage(String message) {

        if (mNode != null && mGoogleApiClient != null) {

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mNode.getId(),
                    WEAR_PATH, message.getBytes())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.d(WEARABLE_MAIN, "Failed message:" + sendMessageResult.getStatus().getStatusCode());
                            } else {
                                Log.d(WEARABLE_MAIN, "Message succeeded");
                            }
                        }
                    });
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            if (node != null && node.isNearby()) {
                                mNode = node;
                                Log.d(WEARABLE_MAIN, "Connected to " + mNode.getDisplayName());

                                String id = mNode.getId();
                                String name = mNode.getDisplayName();

                                Log.d("WEAR CONNECTION", "Connected peer name & ID: " + name + "|" + id);
                            }
                        }
                        if (mNode == null) {
                            Log.d("WEAR CONNECTION", "Not connected");
                        }
                    }
                });

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("WEAR CONNECTION", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("WEAR CONNECTION", "Connection Failed with code: " + connectionResult.getErrorCode());
    }

    //--------------------------------------------------------------------------------
    // OTHER ACTIVITY LIFE CIRCLE METHODS --------------------------------------------
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();

        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (managerAccel != null) {
            managerAccel.unregisterListener((SensorEventListener) AccelListener);
        }
        //if (managerGyro != null) {
          //  managerGyro.unregisterListener((SensorEventListener) GyroListener);
        //}

    }

    @Override
    protected void onResume() {
        super.onResume();

        managerAccel.registerListener(AccelListener, sensorAccel, SensorManager.SENSOR_DELAY_GAME);
        //managerGyro.registerListener(GyroListener, sensorGyro, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();

        managerAccel.unregisterListener(AccelListener);
        //managerGyro.unregisterListener(GyroListener);
    }

    public void responseFromMobile (int responseId) {

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        long[] vibrationPattern = {0,200,0,0};
        final int indexInPatternToRepeat = -1; // Don't repeat
        //vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

        switch (responseId) {

            case 0:
                Log.d("ACTIVITY", "MOVEMENT: UP");
                //darkenButtonBackground(volumeUpButton);

                //buttonRecord.setChecked(true);
                //buttonRecord.callOnClick();

                break;
            case 1:
                Log.d("ACTIVITY", "MOVEMENT: DOWN");
                //darkenButtonBackground(volumeDownButton);

                //buttonRecord.setChecked(true);
                //buttonRecord.callOnClick();

                break;
            case 2:
                Log.d("ACTIVITY", "MOVEMENT: LEFT");
                //darkenButtonBackground(prevButton);

                break;
            case 3:
                Log.d("ACTIVITY", "MOVEMENT: RIGHT");
                //darkenButtonBackground(nextButton);

                break;
            case 4:
                Log.d("ACTIVITY", "MOVEMENT: PUSH");
                //darkenButtonBackground(playPauseButton);

                break;
            case 5:
                Log.d("ACTIVITY", "MOVEMENT: PULL");
                //darkenButtonBackground(playPauseButton);

                break;
            default:
                Log.d("ACTIVITY", "MOVEMENT: NONE");

                break;

        }
    }
}
