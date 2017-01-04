package com.example.mateus.testcastmdtw;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.example.mateus.testcastmdtw.TimedGestureCollectorr.TimedGestureCollector;
import com.example.mateus.testcastmdtw.TimedGestureCollectorr.TimedGestureCollectorListener;
import com.example.mateus.testcastmdtw.TiltMonitor.TiltMonitor;
import com.example.mateus.testcastmdtw.TiltMonitor.TiltMonitorListener;
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

import java.util.List;

public class MainActivity extends WearableActivity
                          implements TiltMonitorListener,
                                     TimedGestureCollectorListener,
                                     GoogleApiClient.ConnectionCallbacks,
                                     GoogleApiClient.OnConnectionFailedListener{

    // JSON parsing
    JSONObject jsonObject;

    // Visual Components
    private ImageButton playPauseButton, volumeDownButton, volumeUpButton, nextButton, prevButton;
    private ToggleButton buttonRecord;
    boolean playButtonState = false;

    // Related to Sending the Message
    private static final String WEARABLE_MAIN = "WearableMain";
    private Node mNode;
    private GoogleApiClient mGoogleApiClient;
    private static final String WEAR_PATH = "/from-wear";

    // Tilt detection and gesture recording class
    private TiltMonitor tiltMonitor;
    private TimedGestureCollector timedGestureCollector;
    private boolean collectingGesture = false;

    public static MainActivity controllerActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setAmbientEnabled();
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

        // Initialize the JSON object
        jsonObject = new JSONObject();

        // Sets the Tilt Monitor
        tiltMonitor = TiltMonitor.getInstance();
        tiltMonitor.setActivity(this);
        tiltMonitor.registerTiltMonitorListener(this);

        // Sets the Sensor Time Series Collector
        timedGestureCollector = TimedGestureCollector.getInstance();
        timedGestureCollector.setActivity(this);
        timedGestureCollector.registerTimeSeriesCollectorListener(this);

        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Starts the sensor collection
                collectingGesture = true;
                timedGestureCollector.startCollecting();

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

    // TiltMonitorListener implementation
    @Override
    public void onTiltDetected() {

        if (!collectingGesture) {
            Log.d("LISTENER", "TILT!!!!!");

            // Feedback on Tilt Detection
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0,200,0,0};
            final int indexInPatternToRepeat = -1; // Don't repeat
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

            // Starts the sensor collection
            collectingGesture = true;
            timedGestureCollector.startCollecting();
        }

    }

    // TimedGestureCollectorListener implementation
    @Override
    public void onSensorTimeSeriesCollected(List<float[]> sensorDataCollected) {

        JSONArray jsonAllAccel = Utils.convertToJSONArray(sensorDataCollected);

        try {
            jsonObject.put("Accel", jsonAllAccel);
            sendMessage(jsonObject.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Reset the JSON objects
        jsonObject = new JSONObject();
        collectingGesture = false;

    }

    @Override
    public void onIdleStateDetected() {
        Log.d("ACTIVITY", "IDLE STATE DETECTED ON GESTURE COLLECTING");
        collectingGesture = false;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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
