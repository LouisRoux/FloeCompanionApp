package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class FloeBLESvc extends Service
{
    private final IBinder bleBinder = new FloeBLEBinder();
    private boolean devicesTethered = false;

    final private BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bleAdapter = bleManager.getAdapter();

    public FloeBLESvc()
    {
        //empty constructor
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        //do stuff
        return bleBinder;
    }

    public class FloeBLEBinder extends Binder
    {
        FloeBLESvc getService()
        {
            return FloeBLESvc.this;
        }
    }




    //testing fctn
    public String Test()
    {
        String s = "it works!";
        return s;
    }
}
