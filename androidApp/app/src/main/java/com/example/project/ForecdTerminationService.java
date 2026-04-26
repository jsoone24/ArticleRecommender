package com.example.project;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * Handles the swipe-killed shutdown path that {@link android.app.Activity#onDestroy}
 * does not see. Uploads the user's reading history before exiting.
 */
public class ForecdTerminationService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("ForecdTermination", "onTaskRemoved - " + rootIntent);
        File recordDb = new File(Config.databasesDir(this), "recorddb");
        new RecordSender(Config.getUserFileUrl(Config.userId(this)), recordDb).execute();
        stopSelf();
    }
}
