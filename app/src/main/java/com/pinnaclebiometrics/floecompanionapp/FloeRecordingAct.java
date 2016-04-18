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
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import java.util.List;
import java.nio.*;

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
    private boolean newRun = true;
    private long runNum;
    private long dataPtNum = 1;

    private FloeDataTransmissionSvc dataService;
    boolean DTSvcBound = false;

    private boolean waitMore = false;

    //the connected bluetooth devices and their adapter
    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private BluetoothDevice bleDeviceLeft = null;
    private BluetoothDevice bleDeviceRight = null;

    public static final String LEFT_NAME = "Left";//used to parse device name and choose which device object to operate on
    public static final String RIGHT_NAME = "Right";//same as LEFT_NAME

    volatile private boolean recordingFlag = false;

    private static boolean bleDeviceLeftConnected = false;
    private static boolean bleDeviceRightConnected = false;

    private Thread recordingWorkThread;

    //TODO: move BLE connection stuff to main menu

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_recording);
        final ImageButton button = (ImageButton) findViewById(R.id.button1);

        db = new FloeRunDatabase(getApplicationContext());
        bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bleManager.getAdapter();
        Log.d(TAG, "initialized Bluetooth Manager and Adapter");

        //Start Data Transmission Service, which in turn starts BLE service. Then, bind to BLESvc as well
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Sent intent to bind to dataTransmissionSvc");
        //i = new Intent(this, FloeBLESvc.class);
        //bindService(i, bleConnection, Context.BIND_AUTO_CREATE);
        //Log.d(TAG, "Sent intent to bind to bleSvc");

        //Register the broadcast receiver for DataTransmissionSvc
        LocalBroadcastManager.getInstance(this).registerReceiver(dataTransmissionBroadcastReceiver, makeDataTransmissionIntentFilter());
        //LocalBroadcastManager.getInstance(this).registerReceiver(BLEBroadcastReceiver, makeBLEIntentFilter());
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
            waitMore = true;
            Log.i(TAG, "BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        //open Device List activity, that scans for devices
        Intent newIntent = new Intent(FloeRecordingAct.this, FloeDeviceListAct.class);
        Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (recordingFlag)
                {
                    button.setBackgroundResource(R.drawable.stop1);
                    recordingFlag = false;
                } else
                {
                    button.setBackgroundResource(R.drawable.record1);
                    recordingFlag = true;
                }
            }
        });

        recordingWorkThread = new Thread()
        {
            @Override
            public void run()
            {
                //start data transfer
                dataService.startDataTransfer();

                while (recordingFlag)
                {
                    recordDataPt();
                }
            }
        };
        //recordingWorkThread.start();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public void onDestroy()
    {
        //set the run duration for the run that has been recorded
        List<FloeDataPt> runPts =  db.getRunDataPts(runNum);
        int runDuration = (int) (runPts.get(runPts.size()-1).getTimeStamp() - runPts.get(0).getTimeStamp());
        FloeRun run = db.getRun(runNum);
        run.setRunDuration(runDuration);
        db.updateRun(run);

        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(dataTransmissionBroadcastReceiver);
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEBroadcastReceiver);
        } catch (Exception ignore)
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
        //dataService.stopSelf();
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
                waitMore=false;
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
                        bleDeviceLeftConnected=false;
                        break;

                    case FloeDataTransmissionSvc.RIGHT_BOOT:
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
                        if(!recordingWorkThread.isAlive())
                        {
                            recordingWorkThread.start();
                        }

                    }else
                    {
                        showMessage("First boot connected successfully. Please connect second boot.");
                        Intent newIntent = new Intent(FloeRecordingAct.this, FloeDeviceListAct.class);
                        Log.d(TAG, "Starting activity with REQUEST_SELECT_DEVICE");
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    }

                }else if(deviceNum == FloeDataTransmissionSvc.RIGHT_BOOT)
                {
                    if(bleDeviceLeftConnected)
                    {
                        showMessage("Second boot connected successfully. Enjoy!");
                        if(!recordingWorkThread.isAlive())
                        {
                            recordingWorkThread.start();
                        }

                    }else
                    {
                        showMessage("First boot connected successfully. Please connect second boot.");
                        Intent newIntent = new Intent(FloeRecordingAct.this, FloeDeviceListAct.class);
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

    private void recordDataPt()
    {
        //call this method in the recording activity worker thread to perform all necessary recording operations
        Log.d(TAG, "recordDataPt()");
        int[] sensorData = extractData(dataService.getRawData());
        int[] centreOfPressure = getCoP(sensorData);
        FloeDataPt dataPt = new FloeDataPt(System.currentTimeMillis(), sensorData, centreOfPressure);

        if(newRun)
        {
            //This is only used for the first data point of a run
            Log.d(TAG, "Creating new run in database");
            long value = dataPt.getTimeStamp();
            FloeRun run = new FloeRun(value);
            runNum = db.createRun(run);
        }

        Log.d(TAG, "Adding point "+dataPtNum+" to run "+runNum);
        dataPt.setRunID(runNum);
        dataPt.setDataPtNum(dataPtNum);
        db.createDataPt(dataPt);
        dataPtNum++;
    }

    public int[] getCoP(int[] sensorData)
    {
        Log.d(TAG, "getCoP()");
        //assigning sensor values
        int BL = sensorData[1];
        int M5L = sensorData[2];
        int M1L = sensorData[0];
        int HL = sensorData[3];
        int BR = sensorData[5];
        int M5R = sensorData[6];
        int M1R = sensorData[4];
        int HR = sensorData[7];
        //TODO: retrieve weight from database to assign
        FloeDataPt temp = db.getDataPt(1);
        int weight = temp.getSensorData(0);

        //TODO: get values for insole distances - relative to 540x444 quadrants

        int dBx = 270;
        int dBy = 400;
        int dM5x = 500;
        int dM5y = 210;
        int dM1x = 270;
        int dM1y = 210;
        int dHx = 360;
        int dHy = 400;

        int CoPx = ( (BR-BL)*dBx + (M5R-M5L)*dM5x + (M1R-M1L)*dM1x + (HR-HL)*dHx)/weight;
        int CoPy = ( (BR+BL)*dBy + (M5R+M5L)*dM5y + (M1R+M1L)*dM1y - (HR+HL)*dHy)/weight;

        Log.d(TAG, "BL="+BL+" BR="+BR+"M5L="+M5L+" M5R="+M5R+"M1L="+M1L+" M1R="+M1R+"HL="+HL+" HR="+HR);
        Log.d(TAG, "CoPx = "+CoPx+" , CoPy = "+CoPy);

        int[] CoP = {CoPx, CoPy};
        return CoP;
    }

    private int[] extractData(byte[][] rawData)
    {
        Log.d(TAG, "extractData()");
        int sensorValue;
        int[] sensorData = new int[8];

        for(int k=0;k<2;k++)
        {
            for (int j = 0; j < 8; j += 2)
            {
                sensorValue = ByteBuffer.wrap(rawData[k]).order(ByteOrder.LITTLE_ENDIAN).getShort(j);//TODO: verify that data is indeed in little-endian
                sensorData[(k*4)+(j/2)] = Linearize(sensorValue);
                Log.i(TAG, "Unpacked data from sensor " + ((k*4)+(j/2)) + ". value = " + sensorValue);
            }
        }
        return sensorData;
    }

    public int Linearize(int v)
    {
        Log.d(TAG, "Linearize("+v+")");
        double r2 = 10000;
        //TODO: input voltage value?
        double inputVoltage = 3;
        double exponent = 1/0.9;

        return (int) Math.pow((inputVoltage/v - 1)*r2, exponent);
    }

    private static IntentFilter makeDataTransmissionIntentFilter()
    //private static IntentFilter makeBLEIntentFilter()
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

    /*

    Old dataTransmissionBroadcastReceiver onReceive()function

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

    private ServiceConnection bleConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            FloeBLESvc.FloeBLEBinder binder = (FloeBLESvc.FloeBLEBinder) service;
            bleService = binder.getService();
            BLESvcBound = true;
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
            BLESvcBound = false;
        }
    };

    private static IntentFilter makeDataTransmissionIntentFilter()
    {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE);
        intentFilter.addAction(FloeDataTransmissionSvc.NEW_DATA_PT_AVAILABLE_NR);
        return intentFilter;
    }
    */
}
//TODO: note down in journal: 1 April studied Android-nRF-UART app by Nordic to figure out stuff about BLE
//TODO: make sure to avoid leaking ServiceConnections