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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class FloeCalibrationAct extends AppCompatActivity {

    FloeDataTransmissionSvc dataService;
    boolean DTSvcBound = false;

    FloeRunDatabase db = new FloeRunDatabase(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_floe_calibration);
        doBindService();
    }

    public void getWeight(View view) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Calibrating... :) ");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(5);
        progress.setProgress(0);
        progress.show();

        if(db.getAllRuns().size() < 1){
            FloeRun testRun = new FloeRun(0);
            testRun.setRunDuration(0);
            testRun.setRunName("WEIGHT");

            long runID = db.createRun(testRun);
            Log.w("FloeCalibrationAct","Database was empty, so new run with runID = "+runID+" was created to store weight. " +
                    "Database size now "+db.getAllRuns().size());
        }


        final Thread t = new Thread() {
            @Override
            public void run() {
                Log.v("FloeCalibrationAct","Inside thread to calculate weight now.");

                int x = 0;
                int sum = 0;
/*
                while(x < 75)
                {
                    int currentSum = 0;
                    int[] currentPoint = dataService.getPoint();
                    for(int i = 0; i < 8; i++)
                    {
                        currentSum += currentPoint[i];
                    }
                    sum += currentSum;
                    x += 1;
                    progress.incrementProgressBy(1);
                }

                sum = sum/75;
                Log.w("FloeCalibrationAct", "Calculated weight = "+sum);

                progress.incrementProgressBy(1);
                progress.dismiss();
                */
                Log.w("FloeCalibrationAct","Updating database with weight value now");
                int[] sensors = {sum,0,0,0,0,0,0,0};
                int[] CoPs = {0,0};
                FloeDataPt weightPt = new FloeDataPt(1, 1, 0, sensors, CoPs);
                weightPt.setDataPtID(1);
                db.updateDataPt(weightPt);

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
                //end test function
            }
        };

        t.start();

        progress.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FloeCalibrationAct.this);
                alertDialogBuilder.setMessage("Your weight has been calibrated!");

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

    public void doBindService(){
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
        DTSvcBound = true;
        Log.w("FloeCalibrationAct", "dataTransSvc bound!");
    }

    @Override
    public void onDestroy()
    {
        //TODO: make sure every bound service gets unbound when its client stops
        super.onDestroy();
        if (DTSvcBound && dataConnection != null)
        {
            unbindService(dataConnection);
            Log.w("FloeCalibrationAct", "dataTransSvc unbound!");
            DTSvcBound = false;
        }
    }

    private ServiceConnection dataConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeDataTransmissionSvc.FloeDTBinder binder = (FloeDataTransmissionSvc.FloeDTBinder) service;
            dataService = binder.getService();
            DTSvcBound = true;
            Log.w("FloeCalibrationAct","dataTransSvc bound in onServiceConnected!");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            DTSvcBound = false;
        }
    };

}
