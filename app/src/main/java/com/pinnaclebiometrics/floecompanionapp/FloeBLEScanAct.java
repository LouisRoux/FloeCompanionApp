package com.pinnaclebiometrics.floecompanionapp;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.logging.Handler;

public class FloeBLEScanAct extends ListActivity
{
    //This activity's purpose is to display the available BLE devices that our app an pair to.
    private BluetoothAdapter bleAdapter;
    private boolean scanning; //true when app is scanning for devices
    private static final int SCAN_PERIOD = 15000; //scan for 15 seconds
    private Handler bleScanHandler;

    private LeDeviceListAdapter bleDeviceListAdapter; //TODO: write out LeDeviceListAdapter class that extends ListAdapter

    private BluetoothAdapter.LeScanCallback bleScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    bleDeviceListAdapter.addDevice(device);
                    bleDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    private void scanBLEDevice(final boolean enable)
    {
        if(enable)
        {
            bleScanHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    scanning = false;
                    bleAdapter.stopLeScan(bleScanCallback);
                }

            }, SCAN_PERIOD);

            scanning = true;
            bleAdapter.startLeScan(bleScanCallback);
        }
        else
        {
            scanning = false;
            bleAdapter.stopLeScan(bleScanCallback);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_blescan);
    }

}
