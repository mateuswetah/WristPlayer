/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cast.refplayer;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.IntroductoryOverlay;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.PendingResults;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultTransform;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.sample.cast.refplayer.MDDTW.MDDTW;
import com.google.sample.cast.refplayer.MDDTW.SensorData;
import com.google.sample.cast.refplayer.queue.QueueDataProvider;
import com.google.sample.cast.refplayer.queue.ui.QueueListViewActivity;
import com.google.sample.cast.refplayer.settings.CastPreference;
import com.google.sample.cast.refplayer.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main activity that displays the list of videos.
 */
public class VideoBrowserActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private CastContext mCastContext;
    private final SessionManagerListener<CastSession> mSessionManagerListener =
            new MySessionManagerListener();
    private static CastSession mCastSession;
    private MenuItem mediaRouteMenuItem;
    private MenuItem mQueueMenuItem;
    private Toolbar mToolbar;
    private IntroductoryOverlay mIntroductoryOverlay;
    private CastStateListener mCastStateListener;

    // GoogleApiClient, needed for starting the watch activity on cast connect
    private static GoogleApiClient mGoogleApiClient;
    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String MOVEMENT_CONFIRMATION_PATH = "/response/MainActivity";

    // MDDTW variables
    private SensorData sensorData;
    public TextView mTextView;
    public static String jsonMessage = "";
    public String recordName;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /* ACTIVITY DEFAULT METHODS --------------------------------------------------------------------
        Usual methods for initializing and setting up the UI in the Video Browser activity
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_browser);
        setupActionBar();

        // Needed for reading template movement json files
        verifyStoragePermissions(this);

        // Setting up cast context
        mCastStateListener = new CastStateListener() {
            @Override

            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {
                    showIntroductoryOverlay();
                }
            }
        };
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);

        // Setting up play services connection for wearable activity instantiation on cast connect.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


    }

    private void setupActionBar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.browse, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
                R.id.media_route_menu_item);
        mQueueMenuItem = menu.findItem(R.id.action_show_queue);
        showIntroductoryOverlay();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_show_queue).setVisible(
                (mCastSession != null) && mCastSession.isConnected());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        if (item.getItemId() == R.id.action_settings) {
            intent = new Intent(VideoBrowserActivity.this, CastPreference.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.action_show_queue) {
            intent = new Intent(VideoBrowserActivity.this, QueueListViewActivity.class);
            startActivity(intent);
        }  else if (item.getItemId() == R.id.action_set_template) {

            showInputDialog();
            return true;
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastContext.onDispatchVolumeKeyEventBeforeJellyBean(event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        mCastContext.addCastStateListener(mCastStateListener);
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        if (mCastSession == null) {
            mCastSession = CastContext.getSharedInstance(this).getSessionManager()
                    .getCurrentCastSession();
        }
        if (mQueueMenuItem != null) {
            mQueueMenuItem.setVisible(
                    (mCastSession != null) && mCastSession.isConnected());
        }
        super.onResume();

    }

    @Override
    protected void onPause() {
        mCastContext.removeCastStateListener(mCastStateListener);
        mCastContext.getSessionManager().removeSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        super.onPause();
    }

    private void showIntroductoryOverlay() {
        if (mIntroductoryOverlay != null) {
            mIntroductoryOverlay.remove();
        }
        if ((mediaRouteMenuItem != null) && mediaRouteMenuItem.isVisible()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mIntroductoryOverlay = new IntroductoryOverlay.Builder(
                            VideoBrowserActivity.this, mediaRouteMenuItem)
                            .setTitleText(getString(R.string.introducing_cast))
                            .setOverlayColor(R.color.primary)
                            .setSingleTime()
                            .setOnOverlayDismissedListener(
                                    new IntroductoryOverlay.OnOverlayDismissedListener() {
                                        @Override
                                        public void onOverlayDismissed() {
                                            mIntroductoryOverlay = null;
                                        }
                                    })
                            .build();
                    mIntroductoryOverlay.show();
                }
            });
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to grant permissions
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /* SAVING MOVEMENTS TEMPLATES ------------------------------------------------------------------
        Movements received from wear are saved in a json file
    */
    private void writeJSONtoFile(String jsonObject) throws IOException {

        FileWriter file;
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + recordName + ".json");
        try {
            file = new FileWriter(path);
            file.write(jsonObject);
            file.flush();
            file.close();
            Toast.makeText(this, "File saved to " + path.getPath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* LOADS MOVEMENTS, PROCESS AND DELEGATE ACTIONS------------------------------------------------
        This method holds the call to MDDTW and the result delegation
    */
    public static void compareMovements() {

        SensorData sensorData;

        Gson gson = new Gson();

        List<SensorData> sensorDataTemplates = new ArrayList<SensorData>(6);
        List<Double> dists = new ArrayList<Double>(6);

        // 1. JSON to Java object, read it from a file.
        try {
            // Load Template files
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_up.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_down.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_left.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_right.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_push.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/new_pull.json"), SensorData.class));

            /* LINEAR_ACCELERATION
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template1.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template2.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template3.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template4.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template5.json"), SensorData.class));
            sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template6.json"), SensorData.class));
            */
            // Load current movement file
            sensorData = gson.fromJson(new FileReader("/sdcard/Download/moviment.json"), SensorData.class);

            if (sensorData.getDataSize() == 0)
                Log.d("ACTIVITY", "RECEIVED EMPTY MOVEMENT");
            else {

                Log.d("SENSOR_X", sensorData.getXData().toString());
                Log.d("SENSOR_Y", sensorData.getYData().toString());
                Log.d("SENSOR_Z", sensorData.getZData().toString());

                double smallestDist = 1000000;
                int smallestIndex = 6;

                for (int i = 0; i < 6; i++) {
                    if (sensorDataTemplates.get(i).getDataSize() == 0) {
                        Log.d("ACTIVITY", "FAILED TO LOAD " + i + "TH TEMPLATE FILE");
                    } else {
                        double dist = new MDDTW(sensorDataTemplates.get(i), sensorData).getDistancia();

                        if (dist < smallestDist) {
                            smallestDist = dist;
                            smallestIndex = i;
                        }
                    }
                }

                switch (smallestIndex) {

                    case 0:
                        Log.d("ACTIVITY", "MOVEMENT: UP");

                        if (mCastSession != null)
                            mCastSession.setVolume(mCastSession.getVolume() + 0.1);
                        break;
                    case 1:
                        Log.d("ACTIVITY", "MOVEMENT: DOWN");

                        if (mCastSession != null)
                            mCastSession.setVolume(mCastSession.getVolume() - 0.1);
                        break;
                    case 2:
                        Log.d("ACTIVITY", "MOVEMENT: LEFT");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() - 30000);
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() - 30000);
                            }
                        }

                        break;
                    case 3:
                        Log.d("ACTIVITY", "MOVEMENT: RIGHT");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() + 30000);
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() + 30000);
                            }
                        }

                        break;
                    case 4:
                        Log.d("ACTIVITY", "MOVEMENT: PUSH");

                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {

                                mCastSession.getRemoteMediaClient().togglePlayback();
                                mCastSession.getRemoteMediaClient().togglePlayback();
                            }
                        }
                        break;
                    case 5:
                        Log.d("ACTIVITY", "MOVEMENT: PULL");
/*
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {

                                mCastSession.getRemoteMediaClient().togglePlayback();
                                mCastSession.getRemoteMediaClient().togglePlayback();
                            }
                        }*/
                        break;
                    default:
                        Log.d("ACTIVITY", "MOVEMENT: NONE");

                        //mCastSession.getRemoteMediaClient().stop();
                        break;

                }

                confirmMovementOnWatch(smallestIndex);

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static void confirmMovementOnWatch(final int movementId) {

        Log.d("MOVEMENT CONFIRMED", "" + movementId);

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    byte[] resp = new byte[1];
                    resp[0] = (byte) movementId;

                    Wearable.MessageApi.sendMessage(mGoogleApiClient , node.getId(), MOVEMENT_CONFIRMATION_PATH , resp).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("GoogleApi", "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            }
                        }
                    });
                }
            }
        });

        return;
    }


/* DELEGATE ACTIONS FROM WEAR UI INSTEAD OF MOVEMENTS ------------------------------------------
        When UI buttons are pressed on wear, instead of movements, this method choose the actions
    */

    private static void delegateUIActions(String button) {

        switch (button) {
            case "PLAY":
                if (mCastSession != null) {
                    if (mCastSession.getRemoteMediaClient() != null) {
                        mCastSession.getRemoteMediaClient().togglePlayback();
                        mCastSession.getRemoteMediaClient().togglePlayback();
                    }
                }
                break;

            case "VOLUME_DOWN":
                if (mCastSession != null)
                    try {
                        mCastSession.setVolume(mCastSession.getVolume() - 0.1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case "VOLUME_UP":
                if (mCastSession != null)
                    try {
                        mCastSession.setVolume(mCastSession.getVolume() + 0.1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case "NEXT":
                if (mCastSession != null) {
                    if (mCastSession.getRemoteMediaClient() != null) {
                        mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() + 30000);
                        mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() + 30000);
                    }
                }
                break;
            case "PREV":
                if (mCastSession != null) {
                    if (mCastSession.getRemoteMediaClient() != null) {
                        mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() - 30000);
                        mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() - 30000);
                    }
                }
                break;
            default:
                Log.d("BUTTON", "NONE OF THE STANDARD OPTIONS WAS REACHED");
                break;
        }

    }

    /* LISTENER SERVICE - SENDS MOVEMENTS DATA FROM WEAR -------------------------------------------
        Here is implemented the actions to be done when the Wear sends a movement message
     */
    public static class ListenerServiceFromWear extends WearableListenerService {

        private static final String WEARPATH = "/from-wear";
        private SensorData sensorData;

        //public String jsonMessage = "";

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            super.onMessageReceived(messageEvent);

            if (messageEvent.getPath().equals(WEARPATH)) {
                jsonMessage = new String(messageEvent.getData());
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(jsonMessage);

                    if (jsonObject.has("button")) { // It's a Button message
                        Log.d("BUTTON", jsonObject.getString("button"));
                        delegateUIActions(jsonObject.getString("button"));

                    } else { // It's a SensorData message
                        try {
                            writeJSONtoFile(jsonMessage);
                            Log.d("MOVEMENT", "Movement instead of button sent from wear.");
                            compareMovements();
                            //compareMovementsOnBackground();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }


        public void compareMovementsOnBackground() {

            Gson gson = new Gson();

            List<SensorData> sensorDataTemplates = new ArrayList<SensorData>(6);
            List<Double> dists = new ArrayList<Double>(6);

            // 1. JSON to Java object, read it from a file.
            try {
                // Load Template files
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template1.json"), SensorData.class));
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template2.json"), SensorData.class));
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template3.json"), SensorData.class));
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template4.json"), SensorData.class));
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template5.json"), SensorData.class));
                sensorDataTemplates.add(gson.fromJson(new FileReader("/sdcard/Download/template6.json"), SensorData.class));

                // Load current movement file
                sensorData = gson.fromJson(new FileReader("/sdcard/Download/moviment.json"), SensorData.class);

                //Log.d("SENSOR_X",sensorData.getXData().toString());
                //Log.d("SENSOR_Y",sensorData.getYData().toString());
                //Log.d("SENSOR_Z",sensorData.getZData().toString());

                double smallestDist = 1000000;
                int smallestIndex = 6;

                for (int i = 0; i < 6; i++) {
                    double dist = new MDDTW(sensorDataTemplates.get(i), sensorData).getDistancia();

                    if (dist < smallestDist) {
                        smallestDist = dist;
                        smallestIndex = i;
                    }
                }

                switch (smallestIndex) {

                    case 0:
                        Log.d("SERVICE", "MOVEMENT: UP");
                        if (mCastSession != null)
                            mCastSession.setVolume(mCastSession.getVolume() + 0.2);
                        break;
                    case 1:
                        Log.d("SERVICE", "MOVEMENT: DOWN");
                        if (mCastSession != null)
                            mCastSession.setVolume(mCastSession.getVolume() - 0.2);
                        break;
                    case 2:
                        Log.d("SERVICE", "MOVEMENT: LEFT");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() - 50000);
                            }
                        }
                        break;
                    case 3:
                        Log.d("SERVICE", "MOVEMENT: RIGHT");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                mCastSession.getRemoteMediaClient().seek(mCastSession.getRemoteMediaClient().getApproximateStreamPosition() + 50000);
                            }
                        }
                        break;
                    case 4:
                        Log.d("SERVICE", "MOVEMENT: PUSH");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                PendingResult<RemoteMediaClient.MediaChannelResult> p2 = mCastSession.getRemoteMediaClient().pause();
                                p2.setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                                    @Override
                                    public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                        Log.d("RESULT", "I'M PAUSED");
                                    }
                                });

                            }
                        }
                        break;
                    case 5:
                        Log.d("SERVICE", "MOVEMENT: PULL");
                        if (mCastSession != null) {
                            if (mCastSession.getRemoteMediaClient() != null) {
                                PendingResult<RemoteMediaClient.MediaChannelResult> p = mCastSession.getRemoteMediaClient().play();
                                p.setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                                    @Override
                                    public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                                        Log.d("RESULT", "I'M PLAYING");
                                    }
                                });

                            }
                        }
                        break;
                    default:
                        Log.d("SERVICE", "MOVEMENT: NONE");
                        //mCastSession.getRemoteMediaClient().stop();
                        break;

                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeJSONtoFile(String jsonObject) throws IOException {

            FileWriter file;
            File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/moviment.json");
            try {
                file = new FileWriter(path);
                file.write(jsonObject);
                file.flush();
                file.close();
//                Toast.makeText(MainActivity.this, "File saved to " + path.getPath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onPeerConnected(com.google.android.gms.wearable.Node peer) {
            super.onPeerConnected(peer);

            String id = peer.getId();
            String name = peer.getDisplayName();

            Log.d("MOBILE", "Connected peer name & ID: " + name + "|" + id);

        }

        @Override
        public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {

            String id = peer.getId();
            String name = peer.getDisplayName();

            Log.d("MOBILE", "Disconnected peer name & ID: " + name + "|" + id);
        }

    }

    /* CAST SESSION MANAGEMENT------------------------------------------
       Handles connecting, resuming, disconnecting to cast.
   */
    private class MySessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == mCastSession) {
                mCastSession = null;
            }
            invalidateOptionsMenu();
            Log.d("CAST SESSION", "ENDED");
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            mCastSession = session;
            invalidateOptionsMenu();
            Log.d("CAST SESSION", "RESUMED");

            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    for (Node node : getConnectedNodesResult.getNodes()) {
                        Wearable.MessageApi.sendMessage(mGoogleApiClient , node.getId() , START_ACTIVITY_PATH , new byte[0]).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.e("GoogleApi", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                }
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            invalidateOptionsMenu();
            Log.d("CAST SESSION", "STARTED");

            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                @Override
                public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                    for (Node node : getConnectedNodesResult.getNodes()) {
                        Wearable.MessageApi.sendMessage(mGoogleApiClient , node.getId() , START_ACTIVITY_PATH , new byte[0]).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    Log.e("GoogleApi", "Failed to send message with status code: "
                                            + sendMessageResult.getStatus().getStatusCode());
                                }
                            }
                        });
                    }
                }
            });
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }


    /* GOOGLE PLAY SERVICES RELATED------------------------------------------
        Necessary for starting the Wear activity once cast is connected.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("GoogleApi", "onConnected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("GoogleApi", "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("GoogleApi", "onConnectionFailed: " + connectionResult);
    }

    protected void showInputDialog() {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(VideoBrowserActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VideoBrowserActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        recordName = editText.getText().toString();
                        try {
                            writeJSONtoFile(jsonMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

}
