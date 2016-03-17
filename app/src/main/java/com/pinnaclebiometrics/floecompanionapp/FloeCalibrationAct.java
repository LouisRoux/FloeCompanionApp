package com.pinnaclebiometrics.floecompanionapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import com.pinnaclebiometrics.floecompanionapp.FloeDataTransmissionSvc.FloeDTBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FloeCalibrationAct extends AppCompatActivity {

    FloeDataTransmissionSvc dataService;

    public void getWeight(View view) {
        //testing stuff; replace with actual thing
        String test = dataService.Test();
        TextView testText = (TextView) findViewById(R.id.testText);
        testText.setText(test);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_calibration);
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection dataConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeDTBinder binder = (FloeDTBinder) service;
            dataService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
