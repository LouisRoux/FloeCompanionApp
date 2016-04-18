package com.pinnaclebiometrics.floecompanionapp;

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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

public class FloeMainMenuAct extends AppCompatActivity {

    FloeRunDatabase db = new FloeRunDatabase(this);


    public static final String TAG = "FloeMainMenuAct";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_1_CONNECTED = 20;
    private static final int UART_PROFILE_2_CONNECTED = 21;
    private static final int UART_PROFILE_DISCONNECTED = 22;
    private static final int STATE_OFF = 10;

    private int state = UART_PROFILE_DISCONNECTED;

    private FloeDataTransmissionSvc dataService;
    boolean DTSvcBound = false;

    //the connected bluetooth devices and their adapter
    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private BluetoothDevice bleDeviceLeft = null;
    private BluetoothDevice bleDeviceRight = null;

    public static final String LEFT_NAME = "Left";//used to parse device name and choose which device object to operate on
    public static final String RIGHT_NAME = "Right";//same as LEFT_NAME

    private static boolean bleDeviceLeftConnected = false;
    private static boolean bleDeviceRightConnected = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_floe_main_menu);

        Button calBtn = (Button) findViewById(R.id.calBtn);
        Button rtBtn = (Button) findViewById(R.id.rtBtn);
        Button recBtn = (Button) findViewById(R.id.recBtn);
        Button revBtn = (Button) findViewById(R.id.revBtn);
        Button reconnectBtn = (Button) findViewById(R.id.reconnectBtn);

        final List<FloeRun> allRuns = db.getAllRuns();

        /*//test - adding run into database
        //TODO: remove testing stuff from here
        if(db.getAllRuns().size() < 1)
        {
            FloeRun testRun = new FloeRun(0);
            testRun.setRunDuration(0);
            testRun.setRunName("WEIGHT");
            long runID = db.createRun(testRun);
            Log.w("FloeMainMenuAct","Database was empty, so new run with runID = "+runID+" was created to store weight. " +
                    "Database size now "+db.getAllRuns().size());

            Log.w("FloeMainMenuAct","Updating database with weight value now");
            int[] sensors = {100,0,0,0,0,0,0,0};
            int[] CoPs = {0,0};
            FloeDataPt weightPt = new FloeDataPt(1, 1, 0, sensors, CoPs);
            weightPt.setDataPtID(1);
            db.updateDataPt(weightPt);
            Log.w("FloeMainMenuAct", "Weight value stored in db at DataPtID=1: "+db.getDataPt(1).getSensorData(0));
        }*/



        db = new FloeRunDatabase(getApplicationContext());
        bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bleManager.getAdapter();
        Log.d(TAG, "initialized Bluetooth Manager and Adapter");

        //Start Data Transmission Service
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Sent intent to bind to dataTransmissionSvc");

        //Register the broadcast receiver for DataTransmissionSvc
        LocalBroadcastManager.getInstance(this).registerReceiver(dataTransmissionBroadcastReceiver, makeDataTransmissionIntentFilter());
        Log.d(TAG, "Registered broadcast receiver");

        //Check if Bluetooth is available and  active, warn or activate if needed
        if (bleAdapter == null)
        {
            Log.e(TAG, "bleAdapter was null");
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!bleAdapter.isEnabled())
        {
            Log.e(TAG, "bleAdapter was not enabled");
            Log.i(TAG, "BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        //open Device List activity, that scans for devices
        Intent newIntent = new Intent(FloeMainMenuAct.this, FloeDeviceListAct.class);
        Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);



        //end test

        calBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(FloeMainMenuAct.this,FloeCalibrationAct.class));
                Log.d("FloeMainMenuAct", "Calibration Activity started'");
            }
        });

        rtBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (db.getAllRuns().size() < 1)
                {
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_SHORT).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else
                {
                    startActivity(new Intent(FloeMainMenuAct.this, FloeRTFeedbackAct.class));
                    Log.d("FloeMainMenuAct", "Realtime feedback activity started'");
                }
            }
        });

        recBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (db.getAllRuns().size() < 1)
                {
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_SHORT).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else
                {
                    startActivity(new Intent(FloeMainMenuAct.this, FloeRecordingAct.class));
                    Log.d("FloeMainMenuAct", "Record activity started'");
                }
            }
        });

        revBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (db.getAllRuns().size() < 1)
                {
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_SHORT).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else
                {
                    startActivity(new Intent(FloeMainMenuAct.this, FloeReviewListAct.class));
                    Log.d("FloeMainMenuAct", "Review stuff activity started'");
                }
            }
        });

        reconnectBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(!bleDeviceLeftConnected || !bleDeviceRightConnected)
                {
                    Intent newIntent = new Intent(FloeMainMenuAct.this, FloeDeviceListAct.class);
                    Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                }
                //TODO: DO IT, LOUIS
            }
        });


    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataTransmissionBroadcastReceiver);
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEBroadcastReceiver);
        }
        catch (Exception ignore)
        {
            Log.e(TAG, ignore.toString());
        }

        /*
        if (BLESvcBound && bleConnection != null)
        {
            unbindService(bleConnection);
            BLESvcBound = false;
            bleService = null;
        }
        */

        if (DTSvcBound && dataConnection != null)
        {
            unbindService(dataConnection);
            DTSvcBound = false;
            dataService = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case REQUEST_SELECT_DEVICE:
                Log.d(TAG, "onActivityResult REQUEST_SELECT_DEVICE");
                if (resultCode == Activity.RESULT_OK && data != null)
                {
                    Log.d(TAG, "RESULT_OK, data!=null");
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = data.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    Log.d(TAG, "deviceName: "+deviceName);

                    if(deviceName.contains(LEFT_NAME))
                    {
                        bleDeviceLeft = bleAdapter.getRemoteDevice(deviceAddress);
                        Log.d(TAG, "bleDeviceLeft = " + bleDeviceLeft + " , dataService = " + dataService);
                        if(dataService.connect(deviceAddress, FloeDataTransmissionSvc.LEFT_BOOT))
                        {
                            Log.d(TAG, "Connection of device LEFT_BOOT was attempted " + deviceAddress);
                        }else
                        {
                            Log.e(TAG, "Somehow connect did not succeed");
                        }

                        //TODO: uncomment following when second board is ready
                        //Start activity to choose second device
                        //Intent newIntent = new Intent(FloeRecordingAct.this, FloeDeviceListAct.class);
                        // Log.d(TAG, "Starting second activity with REQUEST_SELECT_DEVICE");
                        //startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);


                    }else if(deviceName.contains(RIGHT_NAME))
                    {
                        bleDeviceRight = bleAdapter.getRemoteDevice(deviceAddress);
                        Log.d(TAG, "bleDeviceRight = " + bleDeviceRight + " , dataService = " + dataService);
                        if(dataService.connect(deviceAddress, 2))
                        {
                            Log.d(TAG, "Connection of device RIGHT_BOOT was attempted at address "+deviceAddress);
                        }else
                        {
                            Log.e(TAG, "Somehow connect did not succeed");
                        }

                    }else
                    {
                        Log.e(TAG, "Both devices supposedly already connected, why has this been called 3 times?");
                    }
                }else
                {
                    if(resultCode != Activity.RESULT_OK)
                    {
                        Log.e(TAG, "resultCode == RESULT_CANCELLED");
                    }
                    if(data==null)
                    {
                        Log.e(TAG, "data == null");
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
            int deviceNum = intent.getIntExtra(FloeDataTransmissionSvc.EXTRA_DATA, 0);

            if(action.equals(FloeDataTransmissionSvc.ACTION_GATT_CONNECTED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_CONNECTED, device " + deviceNum);
                switch(deviceNum)
                {
                    case FloeDataTransmissionSvc.LEFT_BOOT:
                        bleDeviceLeftConnected=true;
                        break;

                    case FloeDataTransmissionSvc.RIGHT_BOOT:
                        bleDeviceRightConnected=true;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

                if (bleDeviceLeft != null && bleDeviceRight != null)
                {
                    state = UART_PROFILE_2_CONNECTED;
                } else if (bleDeviceLeft != null || bleDeviceRight != null)
                {
                    state = UART_PROFILE_1_CONNECTED;
                } else
                {
                    Log.e(TAG, "No device connected despite just receiving ACTION_GATT_CONNECTED");
                }

            }else if(action.equals(FloeDataTransmissionSvc.ACTION_GATT_DISCONNECTED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_DISCONNECTED, device " + deviceNum);
                if (bleDeviceLeft != null || bleDeviceRight != null)
                {
                    state = UART_PROFILE_1_CONNECTED;
                    dataService.close(deviceNum);
                } else if (bleDeviceLeft == null && bleDeviceRight == null)
                {
                    state = UART_PROFILE_DISCONNECTED;
                    dataService.close(deviceNum);
                } else
                {
                    Log.e(TAG, "More than one device connected despite just receiving ACTION_GATT_DISCONNECTED");
                }

                switch(deviceNum)
                {
                    case FloeDataTransmissionSvc.LEFT_BOOT:
                        showMessage("Connection to "+LEFT_NAME+" Boot was lost unexpectedly. Please reconnect.");
                        bleDeviceLeftConnected=false;
                        break;

                    case FloeDataTransmissionSvc.RIGHT_BOOT:
                        showMessage("Connection to "+RIGHT_NAME+" Boot was lost unexpectedly. Please reconnect.");
                        bleDeviceRightConnected=false;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

            }else if(action.equals(FloeDataTransmissionSvc.ACTION_GATT_SERVICES_DISCOVERED))
            {
                Log.d(TAG, "Received broadcast ACTION_GATT_SERVICES_DISCOVERED");
                dataService.enableTXNotification(deviceNum);

            }else if(action.equals(FloeDataTransmissionSvc.ACTION_DEVICE_READY))
            {
                Log.d(TAG, "Received broadcast ACTION_DEVICE_READY");
                if(deviceNum == FloeDataTransmissionSvc.LEFT_BOOT)
                {
                    if(bleDeviceRightConnected)
                    {
                        showMessage("Second boot connected successfully. Enjoy!");

                    }else
                    {
                        showMessage("First boot connected successfully. Please connect second boot.");
                        Intent newIntent = new Intent(FloeMainMenuAct.this, FloeDeviceListAct.class);
                        Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    }

                }else if(deviceNum == FloeDataTransmissionSvc.RIGHT_BOOT)
                {
                    if(bleDeviceLeftConnected)
                    {
                        showMessage("Second boot connected successfully. Enjoy!");

                    }else
                    {
                        showMessage("First boot connected successfully. Please connect second boot.");
                        Intent newIntent = new Intent(FloeMainMenuAct.this, FloeDeviceListAct.class);
                        Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    }

                }else
                {
                    Log.e(TAG, "Invalid device number: "+ deviceNum);
                }

            }else if(action.equals(FloeDataTransmissionSvc.ACTION_DATA_AVAILABLE))
            {
                Log.d(TAG, "Received broadcast ACTION_DATA_AVAILABLE");
                showMessage("Connection Successful. Receiving data over BLE.");

            }else if(action.equals(FloeDataTransmissionSvc.DEVICE_DOES_NOT_SUPPORT_UART))
            {
                Log.d(TAG, "Received broadcast DEVICE_DOES_NOT_SUPPORT_UART");
                showMessage("Device doesn't support UART. Disconnecting.");
                dataService.disconnect(deviceNum);
                switch(deviceNum)
                {
                    case FloeDataTransmissionSvc.LEFT_BOOT:
                        bleDeviceLeftConnected=false;
                        break;

                    case FloeDataTransmissionSvc.RIGHT_BOOT:
                        bleDeviceRightConnected=false;
                        break;

                    default:
                        Log.e(TAG, "Invalid device number: " + deviceNum);
                        break;
                }

            }else
            {
                Log.e(TAG, "DataTransmissionBroadcastReceiver received an invalid action code");
            }

        }
    };

    private ServiceConnection dataConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            FloeDataTransmissionSvc.FloeDTBinder binder = (FloeDataTransmissionSvc.FloeDTBinder) service;
            dataService = binder.getService();
            DTSvcBound = true;
            Log.d(TAG, "onServiceConnected dataTransmissionService= " + dataService);
            dataService.setDataTransmissionState(FloeDataTransmissionSvc.STATE_RECORDING);
            if (!dataService.initialize())
            {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            DTSvcBound = false;
        }
    };

    private static IntentFilter makeDataTransmissionIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeDataTransmissionSvc.ACTION_GATT_CONNECTED);
        intentFilter.addAction(FloeDataTransmissionSvc.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(FloeDataTransmissionSvc.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(FloeDataTransmissionSvc.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(FloeDataTransmissionSvc.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(FloeDataTransmissionSvc.ACTION_DEVICE_READY);
        return intentFilter;
    }

    private void showMessage(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static boolean isDeviceConnected(int deviceNum)
    {
        switch(deviceNum)
        {
            case FloeDataTransmissionSvc.LEFT_BOOT:
                return bleDeviceLeftConnected;
            case FloeDataTransmissionSvc.RIGHT_BOOT:
                return bleDeviceRightConnected;
            default:
                Log.e(TAG, "Invalid device number passed to isDeviceConnected()");
                return false;
        }
    }
}
