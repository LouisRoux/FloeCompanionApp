package com.pinnaclebiometrics.floecompanionapp;

import android.animation.FloatEvaluator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.hardware.camera2.params.BlackLevelPattern;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class FloeRecordingAct extends AppCompatActivity
{
    public static final String TAG = "FloeRecordingAct";
    public static final String BUNDLE_KEY = "bundled_Data_Pt";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_1_CONNECTED = 20;
    private static final int UART_PROFILE_2_CONNECTED = 21;
    private static final int UART_PROFILE_DISCONNECTED = 22;
    private static final int STATE_OFF = 10;

    private int state = UART_PROFILE_DISCONNECTED;

    private static FloeRunDatabase db;
    private long runNum;
    private long dataPtNum = 1;

    private FloeDataTransmissionSvc dataService;
    private FloeBLESvc bleService;

    //the connected bluetooth devices and their adapter
    private final BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bleAdapter = bleManager.getAdapter();
    private BluetoothDevice bleDevice1 = null;
    private BluetoothDevice bleDevice2 = null;

    private static boolean bleDevice1Connected = false;
    private static boolean bleDevice2Connected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //TODO: set up a basic layout for the recoding activity, including a button to stop/start recording
        setContentView(R.layout.activity_floe_recording);

        db = new FloeRunDatabase(getApplicationContext());

        //Start Data Transmission Service, which in turn starts BLE service. Then, bind to BLESvc as well
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
        i = new Intent(this, FloeBLESvc.class);
        bindService(i, bleConnection, Context.BIND_AUTO_CREATE);

        //Register the broadcast receiver for DataTransmissionSvc
        LocalBroadcastManager.getInstance(this).registerReceiver(dataTransmissionBroadcastReceiver, makeDataTransmissionIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(BLEBroadcastReceiver, makeBLEIntentFilter());

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
        //For now, seems like the solution for multiple devices is to just catch the distinction in the onActivityResult function
        //So, run the startActivityForResult function again
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

        //send the signal to the board to start broadcasting data
        startDataTransfer();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataTransmissionBroadcastReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEBroadcastReceiver);
        } catch (Exception ignore)
        {
            Log.e(TAG, ignore.toString());
        }
        unbindService(bleConnection);
        unbindService(dataConnection);
        dataService.stopSelf();
        dataService = null;
        bleService = null;

    }

    @Override
    protected void onStop()
    {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!bleAdapter.isEnabled())
        {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
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

                    if(bleDevice1==null)
                    {
                        bleDevice1 = bleAdapter.getRemoteDevice(deviceAddress);
                        Log.d(TAG, "... onActivityResultdevice.address==" + bleDevice1 + " bleserviceValue" + bleService);
                        bleService.connect(deviceAddress, 1);

                    }else if(bleDevice2==null)
                    {
                        bleDevice2 = bleAdapter.getRemoteDevice(deviceAddress);
                        Log.d(TAG, "... onActivityResultdevice.address==" + bleDevice2 + " bleserviceValue" + bleService);
                        bleService.connect(deviceAddress, 2);

                    }else
                    {
                        Log.e(TAG, "Both devices supposedly already connected, why has this been called 3 times?");
                    }
                }
                break;

            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK)
                {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else
                {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private final BroadcastReceiver dataTransmissionBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            Bundle b = intent.getBundleExtra(FloeDataTransmissionSvc.EXTRA_DATA);
            FloeDataPt dataPt = b.getParcelable(BUNDLE_KEY);

            if(action.equals(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE))
            {
                Log.d(TAG, "Received broadcast NEW_DATA_PT_AVAILABLE");
                dataPt.setRunID(runNum);
                dataPt.setDataPtNum(dataPtNum);
                db.createDataPt(dataPt);
                dataPtNum++;
            }else if(action.equals(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE_NR))
            {
                //This is only used for the first data point of a run
                Log.d(TAG, "Received broadcast NEW_DATA_PT_AVAILABLE_NR");
                long value = dataPt.getTimeStamp();
                FloeRun run = new FloeRun(value);
                runNum = db.createRun(run);
                dataPt.setRunID(runNum);
                dataPt.setDataPtNum(dataPtNum);
                db.createDataPt(dataPt);
                dataPtNum++;
            }else
            {
                Log.e(TAG, "dataTransmissionBroadcastReceiver received an invalid action code");
            }
        }
    };

    private final BroadcastReceiver BLEBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            int deviceNum = intent.getIntExtra(FloeBLESvc.EXTRA_DATA, 0);

            if(action.equals(FloeBLESvc.ACTION_GATT_CONNECTED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_CONNECTED, device " + deviceNum);
                switch(deviceNum)
                {
                    case 1:
                        bleDevice1Connected=true;
                        break;

                    case 2:
                        bleDevice2Connected=true;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

                if (bleDevice1 != null && bleDevice2 != null)
                {
                    state = UART_PROFILE_2_CONNECTED;
                } else if (bleDevice1 != null || bleDevice2 != null)
                {
                    state = UART_PROFILE_1_CONNECTED;
                } else
                {
                    Log.e(TAG, "No device connected despite just receiving ACTION_GATT_CONNECTED");
                }

            }else if(action.equals(FloeBLESvc.ACTION_GATT_DISCONNECTED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_DISCONNECTED, device " + deviceNum);
                if (bleDevice1 != null || bleDevice2 != null)
                {
                    state = UART_PROFILE_1_CONNECTED;
                    bleService.close(deviceNum);
                } else if (bleDevice1 == null && bleDevice2 == null)
                {
                    state = UART_PROFILE_DISCONNECTED;
                    bleService.close(deviceNum);
                } else
                {
                    Log.e(TAG, "More than one device connected despite just receiving ACTION_GATT_DISCONNECTED");
                }

                switch(deviceNum)
                {
                    case 1:
                        bleDevice1Connected=false;
                        break;

                    case 2:
                        bleDevice2Connected=false;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

            }else if(action.equals(FloeBLESvc.ACTION_GATT_SERVICES_DISCOVERED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_SERVICES_DISCOVERED");
                bleService.enableTXNotification(deviceNum);

            }else if(action.equals(FloeBLESvc.DEVICE_DOES_NOT_SUPPORT_UART))
            {
                Log.d(TAG, "Received broadcast DEVICE_DOES_NOT_SUPPORT_UART");
                showMessage("Device doesn't support UART. Disconnecting.");
                bleService.disconnect(deviceNum);
                switch(deviceNum)
                {
                    case 1:
                        bleDevice1Connected=false;
                        break;

                    case 2:
                        bleDevice2Connected=false;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

            }else
            {
                Log.e(TAG, "BLEBroadcastReceiver received an invalid action code");
            }
        }
    };

    private void startDataTransfer()
    {
        //This function sends the expected values to tell the boards to start transmitting data
        byte[] value = "R00E00000".getBytes();//enable right boot
        bleService.writeRXCharacteristic(value, 1);
        value = "L00E00000".getBytes();//enable left boot
        bleService.writeRXCharacteristic(value, 2);
    }

    private static IntentFilter makeDataTransmissionIntentFilter()
    {
        //TODO: figure out if these IntentFilters work properly. i.e. they don't reject intents b/c of data content
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE);
        intentFilter.addAction(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE_NR);
        return intentFilter;
    }

    private static IntentFilter makeBLEIntentFilter()
    {
        //TODO: figure out if these IntentFilters work properly. i.e. they don't reject intents b/c of data content
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeBLESvc.ACTION_GATT_CONNECTED);
        intentFilter.addAction(FloeBLESvc.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(FloeBLESvc.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(FloeBLESvc.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private ServiceConnection dataConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            FloeDataTransmissionSvc.FloeDTBinder binder = (FloeDataTransmissionSvc.FloeDTBinder) service;
            dataService = binder.getService();
            dataService.setDataTransmissionState(FloeDataTransmissionSvc.STATE_RECORDING);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {

        }
    };

    private ServiceConnection bleConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            FloeBLESvc.FloeBLEBinder binder = (FloeBLESvc.FloeBLEBinder) service;
            bleService = binder.getService();
            Log.d(TAG, "onServiceConnected bleService= " + bleService);
            if (!bleService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            bleService = null;
        }
    };

    private void showMessage(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static boolean isDeviceConnected(int deviceNum)
    {
        switch(deviceNum)
        {
            case 1:
                return bleDevice1Connected;
            case 2:
                return bleDevice2Connected;
            default:
                Log.e(TAG, "Invalid device number passed to isDeviceConnected()");
                return false;
        }
    }
}
//TODO: note down in journal: 1 April studied Android-nRF-UART app by Nordic to figure out stuff about BLE
//TODO: make sure to avoid leaking ServiceConnections