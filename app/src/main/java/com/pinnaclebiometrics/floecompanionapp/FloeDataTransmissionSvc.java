package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Binder;
import android.graphics.Canvas;
import com.pinnaclebiometrics.floecompanionapp.FloeBLESvc.FloeBLEBinder;


public class FloeDataTransmissionSvc extends Service {

    FloeBLESvc bleService;
    private final IBinder dataTranBinder = new FloeDTBinder();

    //constructor
    public FloeDataTransmissionSvc() {
    }

    //fxn to prep data for storage
    public FloeDataPt makeDataPt() {
        FloeDataPt currentPt = new FloeDataPt();

        return currentPt;
    }

    //fxn to calculate CoP
    public int[] getCoP() {
        int CoPx = 0;
        int CoPy = 0;

        //assigning sensor values
        int[] currentPoint = bleService.getPoint();
        int BL = currentPoint[1];
        int M5L = currentPoint[2];
        int M1L = currentPoint[0];
        int HL = currentPoint[3];
        int BR = currentPoint[5];
        int M5R = currentPoint[6];
        int M1R = currentPoint[4];
        int HR = currentPoint[7];
        //TODO: retrieve weight from database to assign
        int weight = 0;

        //TODO: get values for insole distances - relative to 540x425 quadrants
        Canvas canvas = new Canvas();
        int width = canvas.getWidth();
        int length = canvas.getHeight()/2;
        int dBx = 270;
        int dBy = 400;
        int dM5x = 500;
        int dM5y = 210;
        int dM1x = 270;
        int dM1y = 210;
        int dHx = 360;
        int dHy = 400;

        CoPx = ( (BR-BL)*dBx + (M5R-M5L)*dM5x + (M1R-M1L)*dM1x + (HR-HL)*dHx)/weight;
        CoPy = ( (BR+BL)*dBy + (M5R+M5L)*dM5y + (M1R+M1L)*dM1y - (HR+HL)*dHy)/weight;

        int[] CoP = {CoPx, CoPy};
        return CoP;
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
