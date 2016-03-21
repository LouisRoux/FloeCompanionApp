package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Binder;
import com.pinnaclebiometrics.floecompanionapp.FloeBLESvc.FloeBLEBinder;


public class FloeDataTransmissionSvc extends Service {

    FloeBLESvc bleService;
    private final IBinder dataTranBinder = new FloeDTBinder();

    //constructor
    public FloeDataTransmissionSvc() {
    }


    //set up to be bound
    @Override
    public IBinder onBind(Intent intent) {
        return dataTranBinder;
    }

    //happens once: this service is bound to BLE service
    public void onCreate() {
        Intent i = new Intent(this, FloeBLESvc.class);
        bindService(i, bleConnection, Context.BIND_AUTO_CREATE);
    }

    //testing fxn
    public String Test() {
        String s = "it works!";
        return s;
    }

    // create binder
    public class FloeDTBinder extends Binder {
        FloeDataTransmissionSvc getService(){
            return FloeDataTransmissionSvc.this;
        }
    }

    //bind to BLE service
    private ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeBLEBinder binder = (FloeBLEBinder) service;
            bleService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
