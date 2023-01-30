package com.example.project;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import javax.annotation.Nullable;

public class ForecdTerminationService extends Service { //앱이 강제 종료되었을 경우 수행하는 동작이다.
    String android_id;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { //핸들링 하는 부분. 종료직전 서버에 사용자 이용기록을 보낸다.
        Log.e("Error","onTaskRemoved - " + rootIntent);
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        RecordSender recordSender = new RecordSender();
        recordSender.request("http://URL:8080/recommender/GetUserFile/".concat(android_id),"/data/data/com.example.project/databases", "recorddb" );
        stopSelf(); //서비스 종료
    }
}