package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class FloeBLESvc extends Service {

    private final IBinder bleBinder = new FloeBLEBinder();

    public FloeBLESvc() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return bleBinder;
    }

    //testing fxn
    public String Test() {
        String s = "it works!";
        return s;
    }

    public class FloeBLEBinder extends Binder {
        FloeBLESvc getService(){
            return FloeBLESvc.this;
        }
    }
}
