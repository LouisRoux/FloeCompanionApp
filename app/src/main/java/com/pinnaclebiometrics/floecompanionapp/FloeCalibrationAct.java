package com.pinnaclebiometrics.floecompanionapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.app.ProgressDialog;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.content.DialogInterface.OnDismissListener;
import com.pinnaclebiometrics.floecompanionapp.FloeBLESvc.FloeBLEBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


public class FloeCalibrationAct extends AppCompatActivity {

    FloeBLESvc bleService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_floe_calibration);
        Intent i = new Intent(this, FloeBLESvc.class);
        bindService(i, bleConnection, Context.BIND_AUTO_CREATE);
    }

    public void getWeight(View view) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Calibrating... :) ");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(5);
        progress.setProgress(0);
        progress.show();


        final Thread t = new Thread() {
            @Override
            public void run() {
                /*int x = 0;
                int sum = 0;

                while(x < 75)
                {
                    int currentSum = 0;
                    int[] currentPoint = bleService.getPoint();
                    for(int i = 0; i < 8; i++)
                    {
                        currentSum += currentPoint[i];
                    }
                    sum += currentSum;
                    x += 1;
                    progress.incrementProgressBy(1);
                }

                sum = sum/75;

                //todo: store sum in database!
                progress.incrementProgressBy(1);
                progress.dismiss();
                */

                //test function
                for(int i = 0; i < 5; i++){
                    try {
                        Thread.sleep(2000);                 //1000 milliseconds is one second.
                        progress.incrementProgressBy(1);
                        if (progress.getProgress() == progress.getMax())
                        {
                            progress.dismiss();
                        }
                    }
                    catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };

        t.start();

        progress.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FloeCalibrationAct.this);
                alertDialogBuilder.setMessage("Done Calibrating!");

                alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        finish();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
    }



    private ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeBLESvc.FloeBLEBinder binder = (FloeBLESvc.FloeBLEBinder) service;
            bleService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
