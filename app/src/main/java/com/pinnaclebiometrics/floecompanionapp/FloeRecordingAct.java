package com.pinnaclebiometrics.floecompanionapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.camera2.params.BlackLevelPattern;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pinnaclebiometrics.floecompanionapp.FloeDataTransmissionSvc.FloeDTBinder;

public class FloeRecordingAct extends AppCompatActivity
{
    public static final String TAG = "FloeRecordingAct";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private FloeDataTransmissionSvc dataService;

    //the connected bluetooth devices and their adapters
    private final BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bleAdapter = bleManager.getAdapter();
    private BluetoothDevice bleDevice1 = null;
    private BluetoothDevice bleDevice2 = null;


    //testing fxn
    public void getWeight(View view) {
        //testing stuff; replace with actual thing
        String test = dataService.Test();
        TextView testText = (TextView) findViewById(R.id.testText);
        testText.setText(test);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_recording);

        //Start Data Transmission Service, which in turn starts BLE service
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);

        //Check if Bluetooth is available and  active, warn or activate if needed
        if (bleAdapter == null)
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!bleAdapter.isEnabled())
        {
            Log.i(TAG, "BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        //open Device List activity, with popup windows that scan for devices
        Intent newIntent = new Intent(FloeRecordingAct.this, FloeDeviceListAct.class);
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

        //TODO: figure out how to assign different devices to the different adapter and device objects? can we have multiple devices per adapter?
        //For now, seems like the solution is to just catch the distinction in the onActivityResult function
        //So, run the startActivityForResult function again
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null)
                {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    bleDevice1 = bleAdapter.getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + bleDevice1 + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                }

            case REQUEST_ENABLE_BT:

            default:

        }
    }

    private ServiceConnection dataConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeDataTransmissionSvc.FloeDTBinder binder = (FloeDataTransmissionSvc.FloeDTBinder) service;
            dataService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
//TODO: note down in journal: 1 April studied Android-nRF-UART app by Nordic to figure out stuff about BLE
//TODO: Add prompts to connect BLE in this activity, also implement onActivityResult to catch BLE status and display result
//TODO: