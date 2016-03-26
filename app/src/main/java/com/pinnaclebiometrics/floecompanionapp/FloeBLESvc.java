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

    //TODO: figure out if we need multiple adapters/managers (one per foot, or not?)
    final private BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bleAdapter = bleManager.getAdapter();//TODO: Change minimum API version to 18

    public FloeBLESvc()
    {
        //empty constructor
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        //check if bluetooth is enabled and prompt to enable if it isn't
        if(bleAdapter == null || !bleAdapter.isEnabled())
        {
            Log.e("BLESvc", "Bluetooth is not enabled");
            Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBLEIntent, REQUEST_ENABLE_BT);//TODO: put this check in the activity that needs to use BLE
        }

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
