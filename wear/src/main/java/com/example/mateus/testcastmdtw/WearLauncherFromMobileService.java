package com.example.mateus.testcastmdtw;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearLauncherFromMobileService extends WearableListenerService {

    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String MOVEMENT_CONFIRMATION_PATH = "/response/MainActivity";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if(messageEvent.getPath().equals(START_ACTIVITY_PATH)){

            Intent intent = new Intent(this , MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else if (messageEvent.getPath().equals(MOVEMENT_CONFIRMATION_PATH)) {

            MainActivity.controllerActivity.responseFromMobile((int) messageEvent.getData()[0]);

        }
    }
}


