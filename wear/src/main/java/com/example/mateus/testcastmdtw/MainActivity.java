package com.example.mateus.testcastmdtw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import watch.nudge.gesturelibrary.AbstractGestureClientActivity;
import watch.nudge.gesturelibrary.GestureConstants;

public class MainActivity extends AbstractGestureClientActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    // Sensors
    private SensorManager managerAccel, managerGyro;
    private Sensor sensorAccel, sensorGyro;
    private SensorEventListener AccelListener, GyroListener;
    // Visual Components
    private ImageButton playPauseButton, volumeDownButton, volumeUpButton, nextButton, prevButton;
    private ToggleButton buttonRecord;
    boolean playButtonState = false;
    // JSON parsing
    JSONObject jsonObject;
    JSONArray jsonAllAccel, jsonAllGyro;
    int recordNumber = 0;
    // Related to Sending the Message
    private static final String WEARABLE_MAIN = "WearableMain";
    private Node mNode;
    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_PATH = "/from-wear";
    // Strings to be sent over streaming
    private String strAccel;
    private String strGyro;
    private Intent mServiceIntent;

    public static MainActivity controllerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        controllerActivity = this;

        // Verify permissions
        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
        int permissionCheck3 = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        Log.d("ACCESS_WIFI_STATE", "" + (permissionCheck1 == PackageManager.PERMISSION_GRANTED));
        Log.d("ACCESS_NETWORK_STATE", "" + (permissionCheck2 == PackageManager.PERMISSION_GRANTED));
        Log.d("ACCESS_INTERNET", "" + (permissionCheck3 == PackageManager.PERMISSION_GRANTED));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        mServiceIntent = new Intent(this, SensorsService.class);

        // Presto Library Initial Setup
        setSubscribeWindowEvents(true);

        // Associate the XML items for displaying the values
        buttonRecord     = (ToggleButton) findViewById(R.id.ButtonRecord);
        playPauseButton  = (ImageButton)  findViewById(R.id.playPauseButton);
        volumeDownButton = (ImageButton)  findViewById(R.id.volumeDownButton);
        volumeUpButton   = (ImageButton)  findViewById(R.id.volumeUpButton);
        nextButton       = (ImageButton)  findViewById(R.id.nextButton);
        prevButton       = (ImageButton)  findViewById(R.id.prevButton);

        // Set the sensors
        managerAccel = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorAccel = managerAccel.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        managerGyro = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorGyro = managerGyro.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Initialize the JSON object
        jsonObject = new JSONObject();
        jsonAllAccel = new JSONArray();
        jsonAllGyro = new JSONArray();

        // Sensors Listeners
        AccelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (buttonRecord.isChecked()) {
                    JSONArray jsonAccel = new JSONArray();
                    jsonAccel.put(String.valueOf(event.values[0]));
                    jsonAccel.put(String.valueOf(event.values[1]));
                    jsonAccel.put(String.valueOf(event.values[2]));

                    jsonAllAccel.put(jsonAccel);

                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        GyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
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

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (buttonRecord.isChecked()) {

                    try {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                            try {
                                jsonObject = new JSONObject();
                                jsonObject.put("Accel", jsonAllAccel);
                                //jsonObject.put("Gyro", jsonAllGyro);

                                //writeJSONtoFile(jsonObject);
                                sendMessage(jsonObject.toString(2));

                                //reset the JSON objects
                                jsonAllAccel = new JSONArray();
                                //jsonAllGyro = new JSONArray();
                                jsonObject = new JSONObject();


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            buttonRecord.setChecked(false);

                            }
                        }, 1250);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    jsonObject.put("button","PLAY");
                    darkenButtonBackground(playPauseButton);
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
                    darkenButtonBackground(volumeDownButton);
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
                    darkenButtonBackground(volumeUpButton);
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
                    darkenButtonBackground(nextButton);
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
                    darkenButtonBackground(prevButton);
                    sendMessage(jsonObject.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void darkenButtonBackground (final ImageButton imageButton) {
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

    //--------------------------------------------------------------------------------
    // PRESTO LIBRARY METHODS --------------------------------------------------------

    @Override
    public void onSnap() {
        Log.d("PRESTO", "Snapped!");
    }

    @Override
    public void onFlick() {
        Log.d("PRESTO", "Flicked!");
    }

    @Override
    public void onTwist() {
        Log.d("PRESTO", "Twisted!");

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        long[] vibrationPattern = {0,200,0,0};
        final int indexInPatternToRepeat = -1; // Don't repeat
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

        buttonRecord.setChecked(true);
        buttonRecord.callOnClick();

    }

    @Override
    public void onTiltX(float v) {
        Log.d("PRESTO", "TiltedX: " + v + ".");
    }

    @Override
    public void onTilt(float v, float v1, float v2) {
        Log.d("PRESTO", "Tilted - X: " + v + ", Y: " + v1 + ", Z: " + v2 + ".");
    }

    @Override
    public void onGestureWindowClosed() {
        Log.d("PRESTO", "GESTURE WINDOW IS CLOSED");
    }

    @Override
    public ArrayList<GestureConstants.SubscriptionGesture> getGestureSubscpitionList() {

        ArrayList<GestureConstants.SubscriptionGesture> gestures = new ArrayList<GestureConstants.SubscriptionGesture>();
        gestures.add(GestureConstants.SubscriptionGesture.FLICK);
        gestures.add(GestureConstants.SubscriptionGesture.SNAP);
        gestures.add(GestureConstants.SubscriptionGesture.TWIST);
        //gestures.add(GestureConstants.SubscriptionGesture.TILT_X);
        //gestures.add(GestureConstants.SubscriptionGesture.TILT);

        return gestures;
    }

    @Override
    public boolean sendsGestureToPhone() {
        return false;
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
        if (managerGyro != null) {
            managerGyro.unregisterListener((SensorEventListener) GyroListener);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        managerAccel.registerListener(AccelListener, sensorAccel, SensorManager.SENSOR_DELAY_UI);
        managerGyro.registerListener(GyroListener, sensorGyro, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        managerAccel.unregisterListener(AccelListener);
        managerGyro.unregisterListener(GyroListener);
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
