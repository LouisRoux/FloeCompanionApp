package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;
<<<<<<< HEAD
import android.graphics.Canvas;
=======
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

>>>>>>> origin/master
import com.pinnaclebiometrics.floecompanionapp.FloeBLESvc.FloeBLEBinder;


public class FloeDataTransmissionSvc extends Service
{
    public static final String TAG = "FloeDataTransmissionSvc";
    public static final String BUNDLE_KEY = "bundled_Data_Pt";
    public final static String EXTRA_DATA = "com.pinnaclebiometrics.floecompanionapp.EXTRA_DATA";
    public final static String NEW_DATA_PT_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE";
    public final static String NEW_DATA_PT_AVAILABLE_NR = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE_NR";
    public final static String NEW_COP_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_COP_AVAILABLE";
    public final static String NEW_SENSOR_DATA_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_SENSOR_DATA_AVAILABLE";
    public static final int STATE_RT_FEEDBACK = 10;
    public static final int STATE_RECORDING = 20;
    public static final int STATE_CALIBRATING = 30;


    //dataTransmissionState is used to keep track of what to do with incoming data
    private static int dataTransmissionState = 0;

    private boolean newRun = true;
    private int[] sensorData = new int[8];//array to store sensor data temporarily
    private int[] centreOfPressure = new int[2]; //array to store calculated CoP value

    FloeBLESvc bleService;
    private boolean bleSvcBound = false;
    private final IBinder dataTranBinder = new FloeDTBinder();
    FloeRunDatabase db;

    //constructor
    public FloeDataTransmissionSvc()
    {
        //empty
    }

<<<<<<< HEAD
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
        FloeDataPt temp = db.getDataPt(0);
        int weight = temp.getSensorData(0);

        //TODO: get values for insole distances - relative to 540x444 quadrants
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
=======
    //TODO: figure out how to sequentially read from each boot

    private final BroadcastReceiver BLEBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if(action.equals(FloeBLESvc.ACTION_DATA_AVAILABLE))
            {
                Log.d(TAG, "Received broadcast ACTION_DATA_AVAILABLE");
                final byte[] txValue = intent.getByteArrayExtra(FloeBLESvc.EXTRA_DATA);

                //TODO: verify the data-extracting function works
                byte[] dataBytes = {0,0,0,0};
                int sensorValue;
                int baseIndex=0;//baseIndex tells us where to write the data in the sensorData array

                if(txValue[0] == (byte) 0x4C)
                {
                    //data received from left BMH
                    baseIndex=1;
                    Log.d(TAG, "Received data from left BMH");
                }else if(txValue[0] == (byte) 0x52)
                {
                    //Data received from right BMH
                    baseIndex=5;
                    Log.d(TAG, "Received data from right BMH");
                }else
                {
                    //Invalid header
                    Log.e(TAG, "Invalid header code");
                }

                for(int j=1;j<5;j++)
                {
                    dataBytes[2]=txValue[j*2-1];
                    dataBytes[3]=txValue[j*2];
                    sensorValue=0;

                    for (int i = 0; i < 4; i++)
                    {
                        int shift = (4 - 1 - i) * 8;
                        sensorValue += (dataBytes[i] & 0x000000FF) << shift;
                    }
                    sensorData[baseIndex+j-1]=sensorValue;
                    Log.i(TAG, "Unpacked data from sensor " + (baseIndex+j-1) + ". value = " + sensorValue);
                }

                //check if we have enough data to create a DataPt object.
                if(sensorData[0] == -1 || sensorData[4] == -1)
                {
                    //We don't yet have data from each foot
                    Log.d(TAG, "Did not receive data from both boots yet");

                }else
                {
                    //We now have data from both boots, so we decide what to do next based on the activity that launched the service
                    switch (dataTransmissionState)
                    {
                        case STATE_RT_FEEDBACK:
                            Log.d(TAG, "performing operation for STATE_RT_FEEDBACK");
                            calcCoP();
                            //send out broadcast using NEW_COP_AVAILABLE
                            createBroadcast(NEW_COP_AVAILABLE, centreOfPressure);
                            break;
                        case STATE_RECORDING:
                            Log.d(TAG, "performing operation for STATE_RECORDING");
                            calcCoP();
                            //Create dataPt object to send to Recording activity
                            FloeDataPt dataPt = new FloeDataPt(System.currentTimeMillis(), sensorData, centreOfPressure);

                            if (!newRun)
                            {
                                //send out broadcast using NEW_DATA_PT_AVAILABLE
                                createBroadcast(NEW_DATA_PT_AVAILABLE, dataPt);
                            } else
                            {
                                //send out broadcast using NEW_DATA_PT_AVAILABLE_NR
                                createBroadcast(NEW_DATA_PT_AVAILABLE_NR, dataPt);
                                newRun=false;
                            }
                            break;
                        case STATE_CALIBRATING:
                            Log.d(TAG, "performing operation for STATE_CALIBRATING");
                            //send out broadcast using NEW_SENSOR_DATA_AVAILABLE
                            createBroadcast(NEW_SENSOR_DATA_AVAILABLE, sensorData);
                            break;
                        default:
                            //The dataTransmissionState is invalid
                            Log.e(TAG, "invalid dataTransmissionState");
                            break;
                    }
                    //set the flags back so that we can tell when new data has arrived
                    sensorData[0]=-1;
                    sensorData[4]=-1;
                    centreOfPressure[0]=0;
                    centreOfPressure[1]=0;
                }
            }
        }
    };


    private void calcCoP()
    {
        //TODO: write CoP calculation
    }

    private void createBroadcast(final String action, final int[] arrayOfData)
    {
        //This function sends out a broadcast with an int array of the sensor values or CoP, for the Calibrating or RTFeedback activity
        //TODO: verify data is passed properly to calibratingAct and RTFeedbackAct
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, arrayOfData);

        Log.d(TAG, "Sending broadcast " + action);
        //send out the broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createBroadcast(final String action, final FloeDataPt dataPt)
    {
        //This function sends out a broadcast with a dataPt object, for the recording activity
        //TODO: verify data is passed properly to recordingAct
        Bundle b = new Bundle();
        b.putParcelable(BUNDLE_KEY, dataPt);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, b);

        Log.d(TAG, "Sending broadcast " + action);
        //send out the broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

>>>>>>> origin/master

    //set up to be bound
    @Override
    public IBinder onBind(Intent intent)
    {
        return dataTranBinder;
    }

    //happens once: this service is bound to BLE service
    @Override
    public void onCreate()
    {
        Intent i = new Intent(this, FloeBLESvc.class);
<<<<<<< HEAD
        //bindService(i, bleConnection, Context.BIND_AUTO_CREATE);
    }

=======
        bindService(i, bleConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(BLEBroadcastReceiver, makeBLEIntentFilter());
    }

>>>>>>> origin/master
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");
        try
        {
            //unregister the BLE broadcast receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEBroadcastReceiver);
        }catch(Exception ignore)
        {
            Log.e(TAG, ignore.toString());
        }
        //This makes sure to unbind the bleSvc to avoid leaking a ServiceConnection
<<<<<<< HEAD
        //TODO: make sure every bound service gets unbound when its client stops
        bleService.unbindService(bleConnection);
        bleSvcBound = false;
=======
        unbindService(bleConnection);
>>>>>>> origin/master
    }

    // create binder
    public class FloeDTBinder extends Binder
    {
        FloeDataTransmissionSvc getService()
        {
            return FloeDataTransmissionSvc.this;
        }
    }

    //bind to BLE service
    private ServiceConnection bleConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            FloeBLEBinder binder = (FloeBLEBinder) service;
            bleService = binder.getService();
            bleSvcBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            bleSvcBound = false;
        }
    };

    public void setDataTransmissionState(int state)
    {
        //used by the activity that launches this service to indicate what mode of operation to use
        dataTransmissionState = state;
    }

    public static int getDataTransmissionState()
    {
        return dataTransmissionState;
    }

    private IntentFilter makeBLEIntentFilter()
    {
        //TODO: figure out if this IntentFilter works properly. i.e. it doesn't reject intents b/c of data content
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeBLESvc.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
