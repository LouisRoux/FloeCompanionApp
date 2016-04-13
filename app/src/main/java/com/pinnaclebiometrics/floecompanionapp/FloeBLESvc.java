package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



public class FloeBLESvc extends Service {

    private final IBinder bleBinder = new FloeBLEBinder();
 /*   private boolean devicesTethered = false;

    final private BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bleAdapter = bleManager.getAdapter();
*/
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


    //testing fxn
    public String Test() {
        String s = "it works!";
        return s;
    }

    public int Linearize(int v){
        double r2 = 10000;
        //TODO: input voltage value?
        double inputVoltage = 3;
        double exponent = 1/0.9;

        return (int) Math.pow((inputVoltage/v - 1)*r2, exponent);
    }

    public int[] getPoint() {
        int[] point = new int[8];
        /*point[0] = ;
        point[1] = ;
        point[2] = ;
        point[3] = ;
        point[4] = ;
        point[5] = ;
        point[6] = ;
        point[7] = ;*/
        return point;
    }

    public class FloeBLEBinder extends Binder {
        FloeBLESvc getService()
        {
            return FloeBLESvc.this;
        }
    }

}
