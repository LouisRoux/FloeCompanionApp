package com.pinnaclebiometrics.floecompanionapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;

import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.pinnaclebiometrics.floecompanionapp.FloeBLESvc.FloeBLEBinder;

import java.util.UUID;

public class FloeDataTransmissionSvc extends Service
{
    public static final String TAG = "FloeDataTransmissionSvc";
    public final static String EXTRA_DATA = "com.pinnaclebiometrics.floecompanionapp.EXTRA_DATA";

    /*public static final String BUNDLE_KEY = "bundled_Data_Pt";
    public final static String NEW_DATA_PT_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE";
    public final static String NEW_DATA_PT_AVAILABLE_NR = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE_NR";
    public final static String NEW_COP_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_COP_AVAILABLE";
    public final static String NEW_SENSOR_DATA_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_SENSOR_DATA_AVAILABLE";*/

    public static final int STATE_IDLE = 0;
    public static final int STATE_RT_FEEDBACK = 10;
    public static final int STATE_RECORDING = 20;
    public static final int STATE_CALIBRATING = 30;

    public static final int LEFT_BOOT = 1;
    public static final int RIGHT_BOOT = 2;
    public static final String LEFT_ENABLE = "LE";
    public static final String LEFT_DISABLE = "LD";
    public static final String RIGHT_ENABLE = "RE";
    public static final String RIGHT_DISABLE = "RD";

    public final static String ACTION_GATT_CONNECTED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DEVICE_READY = "com.pinnaclebiometrics.floecompanionapp.ACTION_DEVICE_READY";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.pinnaclebiometrics.floecompanionapp.DEVICE_DOES_NOT_SUPPORT_UART";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_1_CONNECTED = 2;
    private static final int STATE_2_CONNECTED = 3;

    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private int connectionState = STATE_DISCONNECTED;

    //dataTransmissionState is used to keep track of what to do with incoming data
    private static int dataTransmissionState = STATE_IDLE;

    //used to store the raw un-linearized sensor data as soon as it is received
    volatile byte[] leftBootRawData = new byte[8];
    volatile byte[] rightBootRawData = new byte[8];
    volatile private boolean acceptNewData = true;

    private int[] sensorData = new int[8];//array to store sensor data temporarily
    private int[] centreOfPressure = new int[2]; //array to store calculated CoP value
    FloeDataPt dataPt = null;//dataPt used to store object before passing it to recordingAct

    volatile private boolean leftBootDataReceived = false;
    volatile private boolean rightBootDataReceived = false;
    private boolean leftBootTransmitting = false;
    private boolean rightBootTransmitting = false;


    //bluetooth objects required to operate properly
    private BluetoothManager bleManager = null;
    private BluetoothAdapter bleAdapter = null;
    private BluetoothDevice bledeviceLeft = null;//maybe we don't need this here
    private String bledeviceLeftAddress = null;
    private BluetoothGatt bleGattLeft = null;
    private BluetoothDevice bledeviceRight = null;//maybe we don't need this here
    private String bledeviceRightAddress = null;
    private BluetoothGatt bleGattRight = null;

    private volatile BluetoothGattCallback bleGattCallback = null;
    private Thread bleWorkerThread = null;
    private  volatile boolean bleWorkerThreadAlive = true;

    @Override
    public void onCreate()
    {
        bleWorkerThread = performOnBackgroundThread(new bleRunnable());
    }

    @Override
    public void onDestroy()
    {
        bleWorkerThreadAlive = false;

    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    private class bleRunnable implements Runnable {
        @Override
        public void run() {
            bleGattCallback = new BluetoothGattCallback()
            {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
                {
                    super.onConnectionStateChange(gatt, status, newState);
                    String intentAction;
                    int deviceNum=0;
                    Log.d(TAG, "OnConnectionStateChange, newState = "+newState);

                    if (newState == BluetoothProfile.STATE_CONNECTED)
                    {
                        intentAction = ACTION_GATT_CONNECTED;

                        if(gatt.equals(bleGattLeft))
                        {
                            deviceNum=LEFT_BOOT;
                            if(bleGattRight!=null)
                            {
                                connectionState = STATE_2_CONNECTED;
                            }else
                            {
                                connectionState = STATE_1_CONNECTED;
                            }
                            createBroadcast(intentAction, deviceNum);
                            Log.i(TAG, "Connected to GATT server 1 (left boot).");
                            // Attempts to discover services after successful connection.
                            Log.i(TAG, "Attempting to start service discovery:" + bleGattLeft.discoverServices());

                        }else if(gatt.equals(bleGattRight))
                        {
                            deviceNum=RIGHT_BOOT;
                            if(bleGattLeft!=null)
                            {
                                connectionState = STATE_2_CONNECTED;
                            }else
                            {
                                connectionState = STATE_1_CONNECTED;
                            }
                            createBroadcast(intentAction, deviceNum);
                            Log.i(TAG, "Connected to GATT server 2 (right boot).");
                            // Attempts to discover services after successful connection.
                            Log.i(TAG, "Attempting to start service discovery:" + bleGattRight.discoverServices());

                        }else
                        {
                            //somehow the connected gatt is neither device 1 nor 2
                            Log.e(TAG, "Connected to GATT Server that is not bleGattLeft nor bleGattRight");
                        }

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                    {
                        intentAction = ACTION_GATT_DISCONNECTED;

                        if(bleGattLeft==null && bleGattRight==null)
                        {
                            connectionState = STATE_DISCONNECTED;
                            Log.i(TAG, "Disconnected from last GATT server.");

                            switch(FloeDataTransmissionSvc.getDataTransmissionState())
                            {
                                //TODO: modify RTFeedback and Calibration Activities to include isDeviceConnected(), then un-comment
                        /*case FloeDataTransmissionSvc.STATE_RT_FEEDBACK:
                            if(FloeRTFeedbackAct.isDeviceConnected(LEFT_BOOT))
                            {
                                deviceNum=LEFT_BOOT;
                            }else if(FloeRTFeedbackAct.isDeviceConnected(RIGHT_BOOT))
                            {
                                deviceNum=RIGHT_BOOT;
                            }
                            break;*/

                                case FloeDataTransmissionSvc.STATE_RECORDING:
                                    if(FloeRecordingAct.isDeviceConnected(LEFT_BOOT))
                                    {
                                        deviceNum=LEFT_BOOT;
                                    }else if(FloeRecordingAct.isDeviceConnected(RIGHT_BOOT))
                                    {
                                        deviceNum=RIGHT_BOOT;
                                    }
                                    break;

                        /*case FloeDataTransmissionSvc.STATE_CALIBRATING:
                            if(FloeCalibrationAct.isDeviceConnected(LEFT_BOOT))
                            {
                                deviceNum=LEFT_BOOT;
                            }else if(FloeCalibrationAct.isDeviceConnected(RIGHT_BOOT))
                            {
                                deviceNum=RIGHT_BOOT;
                            }
                            break;*/

                                default:
                                    break;
                            }

                        }else if(bleGattLeft!=null)
                        {
                            connectionState = STATE_1_CONNECTED;
                            deviceNum=RIGHT_BOOT;
                            Log.i(TAG, "Disconnected from GATT server 2 (right boot.");

                        }else if(bleGattRight!=null)
                        {
                            connectionState = STATE_1_CONNECTED;
                            deviceNum=LEFT_BOOT;
                            Log.i(TAG, "Disconnected from GATT server 1 (left boot).");

                        }else
                        {
                            Log.e(TAG, "Disconnected from a GATT server, but bleGattLeft and bleGattRight still not null");
                        }

                        createBroadcast(intentAction, deviceNum);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status)
                {
                    int deviceNum=0;
                    Log.d(TAG, "onServicesDiscovered()");
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        if(gatt.equals(bleGattLeft))
                        {
                            Log.w(TAG, "bleGattLeft = " + bleGattLeft);
                            deviceNum=LEFT_BOOT;

                        }else if(gatt.equals(bleGattRight))
                        {
                            Log.w(TAG, "bleGattRight = " + bleGattRight);
                            deviceNum=RIGHT_BOOT;

                        }else
                        {
                            Log.e(TAG, "onServicesDiscovered received GATT_SUCCESS, but bleGattLeft and bleGattRight are not the discovered service");
                        }
                        createBroadcast(ACTION_GATT_SERVICES_DISCOVERED, deviceNum);

                    } else
                    {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
                {
                    Log.d(TAG, "onCharacteristicRead()");
                    if (status == BluetoothGatt.GATT_SUCCESS)
                    {
                        createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
                        //TODO: see if we need to add more here
                        //probably not
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
                {

                    if(dataTransmissionState == STATE_IDLE)
                    {
                        //We don't want to do anything with the data
                        return;
                    }
                    Log.d(TAG, "onCharacteristicChanged(");
                    byte[] receivedChar = characteristic.getValue();
                    Log.d(TAG, "characteristic: "+receivedChar[0]+" "+receivedChar[1]+" "+receivedChar[2]+" "+receivedChar[3]+" "+receivedChar[4]+" "+receivedChar[5]+" "+receivedChar[6]+" "+receivedChar[7]+" "+receivedChar[8]);

                    if(receivedChar[0] == (byte) 0x4C)
                    {
                        //data received from left BMH
                        Log.d(TAG, "Received data from left BMH");
                        for(int i=1;i<9;i++)
                        {
                            leftBootRawData[i-1] = receivedChar[i];
                        }
                        leftBootDataReceived=true;

                    }else if(receivedChar[0] == (byte) 0x52)
                    {
                        Log.d(TAG, "Received data from right BMH");
                        for(int i=1;i<9;i++)
                        {
                            rightBootRawData[i-1] = receivedChar[i];
                        }
                        rightBootDataReceived=true;

                    }else
                    {
                        //Invalid header
                        Log.e(TAG, "Invalid header code");
                    }

                    Log.d(TAG, "onCharacteristicChanged() completed correctly");

                    //extractData(characteristic.getValue());

                    //createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
                {
                    Log.d(TAG, "onDescriptorWrite()");
                    //check if both devices are ready, then send the order to start transmission

                    if(bleGattLeft!=null)
                    {
                        if(bleGattRight != null)
                        {
                            createBroadcast(ACTION_DEVICE_READY, RIGHT_BOOT);
                            Log.d(TAG, "starting data transfer");
                            startDataTransfer();//TODO: make sure this is in the right place, potentially put in individual activities
                        }else
                        {
                            //send broadcast for activity to prompt choice for second boot
                            createBroadcast(ACTION_DEVICE_READY, LEFT_BOOT);
                        }
                    }else if(bleGattRight != null)
                    {
                        //send broadcast for activity to prompt choice for second boot
                        createBroadcast(ACTION_DEVICE_READY, RIGHT_BOOT);
                    }
                    else
                    {
                        Log.e(TAG, "Both Gatts are still null");
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
                {
                    Log.d(TAG, "onCharacteristicWrite()");
                    byte[] sChar = characteristic.getValue();
                    char char1 = (char) sChar[0];
                    char char2 = (char) sChar[1];

                    if(char1 == 'L')
                    {
                        if(char2 == 'L')
                        {
                            Log.d(TAG, "Left boot now transmitting data");
                            if (rightBootTransmitting)
                            {
                                //both boots are transmitting data
                                Log.d(TAG, "Both boots now transmitting data");
                            } else
                            {
                                byte[] value = RIGHT_ENABLE.getBytes();//enable left boot
                                Log.d(TAG, "writeRXCharacteristic (value " + RIGHT_ENABLE + ", deviceNum RIGHT_BOOT)");
                                writeRXCharacteristic(value, RIGHT_BOOT);
                            }

                        }else if(char2 == 'D')
                        {
                            Log.d(TAG, "Left boot no longer transmitting data");
                            if (rightBootTransmitting)
                            {
                                //both boots are transmitting data
                                Log.d(TAG, "Right boot still transmitting data");
                                byte[] value = RIGHT_DISABLE.getBytes();//enable left boot
                                Log.d(TAG, "writeRXCharacteristic (value " + RIGHT_DISABLE + ", deviceNum RIGHT_BOOT)");
                                writeRXCharacteristic(value, RIGHT_BOOT);
                            } else
                            {
                                Log.d(TAG, "No boot still transmitting data");
                            }

                        }else
                        {
                            Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
                        }
                    }else if(char1 == 'R')
                    {

                        if(char2 == 'D')
                        {
                            Log.d(TAG, "Right boot no longer transmitting data");
                            if (leftBootTransmitting)
                            {
                                //both boots are transmitting data
                                Log.d(TAG, "Left boot still transmitting data");
                                byte[] value = LEFT_DISABLE.getBytes();//enable left boot
                                Log.d(TAG, "writeRXCharacteristic (value " + LEFT_DISABLE + ", deviceNum RIGHT_BOOT)");
                                writeRXCharacteristic(value, LEFT_BOOT);
                            } else
                            {
                                Log.d(TAG, "No boot still transmitting data");
                            }
                        }else if(char2 == 'E')
                        {
                            Log.d(TAG, "Right boot now transmitting data");
                            if (leftBootTransmitting)
                            {
                                //both boots are transmitting data
                                Log.d(TAG, "Both boots now transmitting data");
                            } else
                            {
                                byte[] value = LEFT_ENABLE.getBytes();//enable left boot
                                Log.d(TAG, "writeRXCharacteristic (value " + LEFT_ENABLE + ", deviceNum RIGHT_BOOT)");
                                writeRXCharacteristic(value, LEFT_BOOT);
                            }

                        }else
                        {
                            Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
                        }

                    }else
                    {
                        Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
                    }
                }
            };
            while (bleWorkerThreadAlive)
            {
                // WAIT
            }

        }
    }

    /*
    FloeBLESvc bleService;
    private boolean bleSvcBound = false;
    */

    private final IBinder dataTranBinder = new FloeDTBinder();
    FloeRunDatabase db;


    //constructor
    public FloeDataTransmissionSvc()
    {
        //empty
    }

    //fxn to calculate CoP
    /*public int[] getCoP()
    {
        Log.d(TAG, "getCoP()");
        //assigning sensor values
        int[] currentPoint = sensorData;
        int BL = currentPoint[1];
        int M5L = currentPoint[2];
        int M1L = currentPoint[0];
        int HL = currentPoint[3];
        int BR = currentPoint[5];
        int M5R = currentPoint[6];
        int M1R = currentPoint[4];
        int HR = currentPoint[7];
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

    private void extractData()
    {
        //TODO: verify the data-extracting function works
        Log.d(TAG, "extractData()");
        byte[] dataBytes = {0,0,0,0};
        int sensorValue;
        int baseIndex=0;//baseIndex tells us where to write the data in the sensorData array. We start with left boot

        for(int j=0;j<4;j++)
        {
            dataBytes[2]=leftBootRawData[j*2];
            dataBytes[3]=leftBootRawData[j*2+1];
            sensorValue=0;

            for (int i = 0; i < 4; i++)
            {
                int shift = (4 - 1 - i) * 8;
                sensorValue += (dataBytes[i] & 0x000000FF) << shift;
            }
            sensorData[baseIndex+j] = Linearize(sensorValue);
            Log.i(TAG, "Unpacked data from sensor " + (baseIndex+j) + ". value = " + sensorValue);
        }

        baseIndex=4;
        for(int j=0;j<4;j++)
        {
            dataBytes[2]=rightBootRawData[j*2];
            dataBytes[3]=rightBootRawData[j*2+1];
            sensorValue=0;

            for (int i = 0; i < 4; i++)
            {
                int shift = (4 - 1 - i) * 8;
                sensorValue += (dataBytes[i] & 0x000000FF) << shift;
            }
            sensorData[baseIndex+j] = Linearize(sensorValue);
            Log.i(TAG, "Unpacked data from sensor " + (baseIndex+j) + ". value = " + sensorValue);
        }

        //We now have data from both boots, so we decide what to do next based on the activity
        switch (dataTransmissionState)
        {
            case STATE_RT_FEEDBACK:
                Log.d(TAG, "performing operation for STATE_RT_FEEDBACK");
                getCoP();
                //send out broadcast using NEW_COP_AVAILABLE
                //createBroadcast(NEW_COP_AVAILABLE, centreOfPressure);
                //newDataPointReady=true;
                break;
            case STATE_RECORDING:
                Log.d(TAG, "performing operation for STATE_RECORDING");
                getCoP();
                //Create dataPt object for Recording activity
                FloeDataPt dataPt = new FloeDataPt(System.currentTimeMillis(), sensorData, centreOfPressure);
                //newDataPointReady=true;
                break;

            case STATE_CALIBRATING:
                Log.d(TAG, "performing operation for STATE_CALIBRATING");
                //newDataPointReady=true;
                //send out broadcast using NEW_SENSOR_DATA_AVAILABLE
                //createBroadcast(NEW_SENSOR_DATA_AVAILABLE, sensorData);
                break;
            default:
                //The dataTransmissionState is invalid
                Log.e(TAG, "invalid dataTransmissionState");
                break;
        }
        //set the flags back so that we can tell when new data has arrived
        leftBootDataReceived = false;
        rightBootDataReceived = false;
    }
    //TODO: figure out how to sequentially read from each boot

    public int Linearize(int v)
    {
        Log.d(TAG, "Linearize("+v+")");
        double r2 = 10000;
        //TODO: input voltage value?
        double inputVoltage = 3;
        double exponent = 1/0.9;

        return (int) Math.pow((inputVoltage/v - 1)*r2, exponent);
    }*/

    public synchronized byte[] getRawData()
    {
        Log.d(TAG, "getRawData()");
        if(leftBootDataReceived && rightBootDataReceived)
        {
            byte[] rawData = new byte[16];

            for (int i = 0; i < 8; i++)
            {
                rawData[i]=leftBootRawData[i];
            }
            for (int i=1;i<8;i++)
            {
                rawData[i+8]=rightBootRawData[i];
            }

            leftBootDataReceived=false;
            rightBootDataReceived=false;

            return rawData;
        }
        else
        {
            Log.d(TAG, "No new data point ready");
            return null;
        }
    }

    public boolean initialize()
    {
        Log.d(TAG, "initialize()");
        if (bleManager == null)
        {
            bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bleManager == null)
            {
                Log.e(TAG, "Unable to initialize bleManager.");
                return false;
            }
        }

        bleAdapter = bleManager.getAdapter();
        if (bleAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(String address, int deviceNum)
    {
        Log.d(TAG, "connect(address " + address + ", deviceNum " + deviceNum + ")");
        if (bleAdapter == null || address == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = bleAdapter.getRemoteDevice(address);
        if (device == null)
        {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        switch (deviceNum)
        {
            case 1:
                // Previously connected device.  Try to reconnect.
                if (bledeviceLeftAddress != null && address.equals(bledeviceLeftAddress) && bleGattLeft != null)
                {
                    Log.d(TAG, "Trying to use an existing bleGattLeft for connection.");
                    if (bleGattLeft.connect())
                    {
                        connectionState = STATE_CONNECTING;
                        return true;

                    } else
                    {
                        return false;
                    }
                }

                // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
                Log.d(TAG, "Trying to create a new connection for bleGattLeft.");
                bleGattLeft = device.connectGatt(this, false, bleGattCallback);
                Log.d(TAG, "bleGattLeft = " + bleGattLeft);
                bledeviceLeftAddress = address;
                Log.d(TAG, "bledeviceLeftAddress = " + bledeviceLeftAddress);
                connectionState = STATE_CONNECTING;
                return true;

            case RIGHT_BOOT:

                // Previously connected device.  Try to reconnect.
                if (bledeviceRightAddress != null && address.equals(bledeviceRightAddress) && bleGattRight != null)
                {
                    Log.d(TAG, "Trying to use an existing bleGattRight for connection.");
                    if (bleGattRight.connect())
                    {
                        connectionState = STATE_CONNECTING;
                        return true;

                    } else
                    {
                        return false;
                    }
                }

                // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
                bleGattRight = device.connectGatt(this, false, bleGattCallback);
                Log.d(TAG, "Trying to create a new connection for bleGattRight.");
                bledeviceRightAddress = address;
                connectionState = STATE_CONNECTING;
                return true;

            default:
                Log.e(TAG, "Invalid device number passed to connect()");
                return false;
        }
    }

    public void disconnect(int deviceNum)
    {
        Log.d(TAG, "disconnect("+deviceNum+")");
        switch (deviceNum)
        {
            case 1:
                if (bleAdapter == null || bleGattLeft == null)
                {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                    return;
                }
                bleGattLeft.disconnect();
                break;
            case RIGHT_BOOT:
                if (bleAdapter == null || bleGattRight == null)
                {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                    return;
                }
                bleGattRight.disconnect();
                break;
            default:
                Log.e(TAG, "Invalid device number passed to disconnect()");
                break;
        }
    }

    public void close(int deviceNum)
    {
        Log.d(TAG, "close("+deviceNum+")");
        switch(deviceNum)
        {
            case LEFT_BOOT:
                if (bleGattLeft == null)
                {
                    return;
                }
                Log.w(TAG, "bleGattLeft closed");
                bledeviceLeftAddress = null;
                bleGattLeft.close();
                bleGattLeft = null;
                break;

            case RIGHT_BOOT:
                if (bleGattRight == null)
                {
                    return;
                }
                Log.w(TAG, "bleGattLeft closed");
                bledeviceRightAddress = null;
                bleGattRight.close();
                bleGattRight = null;
                break;

            default:
                Log.e(TAG, "Invalid device number passed to close()");
                break;
        }
    }

    public void writeRXCharacteristic(byte[] value, int deviceNum)
    {
        Log.d(TAG, "writeRXCharacteristic "+value[0]+" "+value[1]+" , deviceNum "+deviceNum);
        switch (deviceNum)
        {
            case LEFT_BOOT:
                BluetoothGattService RxService = bleGattLeft.getService(RX_SERVICE_UUID);
                Log.e(TAG, "bleGattLeft null " + bleGattLeft);
                Log.d(TAG, "bleGattLeft RxService = " + RxService.toString());
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                Log.d(TAG, "bleGattLeft RxChar = "+RxChar.toString());
                if (RxChar == null)
                {
                    Log.e(TAG,"Rx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar.setValue(value);
                boolean status = bleGattLeft.writeCharacteristic(RxChar);

                Log.d(TAG, "write TXchar - status = " + status);
                leftBootTransmitting=true;
                break;

            case RIGHT_BOOT:
                RxService = bleGattRight.getService(RX_SERVICE_UUID);
                Log.e(TAG, "bleGattRight null" + bleGattLeft);
                Log.d(TAG, "bleGattRight RxService = " + RxService.toString());
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                Log.d(TAG, "bleGattRight RxChar = "+RxChar.toString());
                if (RxChar == null)
                {
                    Log.e(TAG,"Rx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar.setValue(value);
                status = bleGattRight.writeCharacteristic(RxChar);

                Log.d(TAG, "write TXchar - status=" + status);
                rightBootTransmitting=true;
                break;

            default:
                Log.e(TAG, "Invalid device number passed to writeRXCharacteristic()");
                break;
        }
    }

    public void enableTXNotification(int deviceNum)
    {
        Log.d(TAG, "enableTXNotification(deviceNum "+deviceNum+")");
        switch(deviceNum)
        {
            case LEFT_BOOT:
                BluetoothGattService RxService = bleGattLeft.getService(RX_SERVICE_UUID);
                Log.d(TAG, "RxService = " + RxService.toString());
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
                Log.d(TAG, "TxChar = " + TxChar.toString());
                if (TxChar == null)
                {
                    Log.e(TAG, "Tx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                Log.d(TAG, "set bleGattLeft Characteristic Notification " + TxChar.toString());
                bleGattLeft.setCharacteristicNotification(TxChar, true);

                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Log.d(TAG, "writing bleGattLeft descriptor " + descriptor);
                bleGattLeft.writeDescriptor(descriptor);
                //writeDescriptor() will call bleGattCallback.onDescriptorWrite() upon successful descriptor write

                break;

            case RIGHT_BOOT:
                RxService = bleGattRight.getService(RX_SERVICE_UUID);
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
                if (TxChar == null)
                {
                    Log.e(TAG, "Tx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                bleGattRight.setCharacteristicNotification(TxChar, true);

                descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bleGattRight.writeDescriptor(descriptor);

                break;

            default:
                Log.e(TAG, "Invalid device number passed to enableTXNotification()");
                break;
        }
    }

    public void startDataTransfer()
    {
        //This function sends the expected values to tell the boards to start transmitting data
        Log.d(TAG, "startDataTransfer()");

        byte[] value = RIGHT_ENABLE.getBytes();//enable right boot
        Log.d(TAG, "writeRXCharacteristic (value "+RIGHT_ENABLE+", deviceNum RIGHT_BOOT)");
        writeRXCharacteristic(value, RIGHT_BOOT);
    }

    public void stopDataTransfer()
    {
        //This function sends the expected values to tell the boards to start transmitting data
        Log.d(TAG, "stopDataTransfer()");

        byte[] value = RIGHT_DISABLE.getBytes();//enable right boot
        Log.d(TAG, "writeRXCharacteristic (value "+RIGHT_DISABLE+", deviceNum RIGHT_BOOT)");
        writeRXCharacteristic(value, RIGHT_BOOT);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic, int deviceNum)
    {
        if (bleAdapter == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        switch (deviceNum)
        {
            case LEFT_BOOT:
                if (bleGattLeft == null)
                {
                    Log.w(TAG, "bleGattLeft not initialized");
                    return;
                }
                bleGattLeft.readCharacteristic(characteristic);
                break;

            case RIGHT_BOOT:
                if (bleGattRight == null)
                {
                    Log.w(TAG, "bleGattRight not initialized");
                    return;
                }
                bleGattRight.readCharacteristic(characteristic);
                break;

            default:
                Log.e(TAG, "Invalid device number passed to enableTXNotification()");
                break;
        }
    }

    private void createBroadcast(final String action, final BluetoothGattCharacteristic characteristic)
    {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (TX_CHAR_UUID.equals(characteristic.getUuid()))
        {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else
        {
            //TODO: figure out if we need something here
            Log.e(TAG, "TX_CHAR_UUID.equals(characteristic.getUuid()) returned false");
        }

        Log.d(TAG, "Sending broadcast " + action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createBroadcast(final String action, int deviceNum)
    {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, deviceNum);
        Log.d(TAG, "Sending broadcast " + action +", device "+ deviceNum);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }


    //set up to be bound
    @Override
    public IBinder onBind(Intent intent)
    {
        return dataTranBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the Activity is disconnected from the Data Transmission Service.
        if(bleGattLeft!=null)
        {
            close(LEFT_BOOT);
        }
        if(bleGattRight!=null)
        {
            close(RIGHT_BOOT);
        }
        return super.onUnbind(intent);
    }

    // create binder
    public class FloeDTBinder extends Binder
    {
        FloeDataTransmissionSvc getService()
        {
            return FloeDataTransmissionSvc.this;
        }
    }

    public void setDataTransmissionState(int state)
    {
        //used by the activity that launches this service to indicate what mode of operation to use
        dataTransmissionState = state;
    }

    public static int getDataTransmissionState()
    {
        return dataTransmissionState;
    }


    //GattCallback before it was moved to a separate thread
    /*    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            int deviceNum=0;
            Log.d(TAG, "OnConnectionStateChange, newState = "+newState);

            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                intentAction = ACTION_GATT_CONNECTED;

                if(gatt.equals(bleGattLeft))
                {
                    deviceNum=LEFT_BOOT;
                    if(bleGattRight!=null)
                    {
                        connectionState = STATE_2_CONNECTED;
                    }else
                    {
                        connectionState = STATE_1_CONNECTED;
                    }
                    createBroadcast(intentAction, deviceNum);
                    Log.i(TAG, "Connected to GATT server 1 (left boot).");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + bleGattLeft.discoverServices());

                }else if(gatt.equals(bleGattRight))
                {
                    deviceNum=RIGHT_BOOT;
                    if(bleGattLeft!=null)
                    {
                        connectionState = STATE_2_CONNECTED;
                    }else
                    {
                        connectionState = STATE_1_CONNECTED;
                    }
                    createBroadcast(intentAction, deviceNum);
                    Log.i(TAG, "Connected to GATT server 2 (right boot).");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + bleGattRight.discoverServices());

                }else
                {
                    //somehow the connected gatt is neither device 1 nor 2
                    Log.e(TAG, "Connected to GATT Server that is not bleGattLeft nor bleGattRight");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;

                if(bleGattLeft==null && bleGattRight==null)
                {
                    connectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from last GATT server.");

                    switch(FloeDataTransmissionSvc.getDataTransmissionState())
                    {
                        //TODO: modify RTFeedback and Calibration Activities to include isDeviceConnected(), then un-comment
                        *//*case FloeDataTransmissionSvc.STATE_RT_FEEDBACK:
                            if(FloeRTFeedbackAct.isDeviceConnected(LEFT_BOOT))
                            {
                                deviceNum=LEFT_BOOT;
                            }else if(FloeRTFeedbackAct.isDeviceConnected(RIGHT_BOOT))
                            {
                                deviceNum=RIGHT_BOOT;
                            }
                            break;*//*

                        case FloeDataTransmissionSvc.STATE_RECORDING:
                            if(FloeRecordingAct.isDeviceConnected(LEFT_BOOT))
                            {
                                deviceNum=LEFT_BOOT;
                            }else if(FloeRecordingAct.isDeviceConnected(RIGHT_BOOT))
                            {
                                deviceNum=RIGHT_BOOT;
                            }
                            break;

                        *//*case FloeDataTransmissionSvc.STATE_CALIBRATING:
                            if(FloeCalibrationAct.isDeviceConnected(LEFT_BOOT))
                            {
                                deviceNum=LEFT_BOOT;
                            }else if(FloeCalibrationAct.isDeviceConnected(RIGHT_BOOT))
                            {
                                deviceNum=RIGHT_BOOT;
                            }
                            break;*//*

                        default:
                            break;
                    }

                }else if(bleGattLeft!=null)
                {
                    connectionState = STATE_1_CONNECTED;
                    deviceNum=RIGHT_BOOT;
                    Log.i(TAG, "Disconnected from GATT server 2 (right boot.");

                }else if(bleGattRight!=null)
                {
                    connectionState = STATE_1_CONNECTED;
                    deviceNum=LEFT_BOOT;
                    Log.i(TAG, "Disconnected from GATT server 1 (left boot).");

                }else
                {
                    Log.e(TAG, "Disconnected from a GATT server, but bleGattLeft and bleGattRight still not null");
                }

                createBroadcast(intentAction, deviceNum);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            int deviceNum=0;
            Log.d(TAG, "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if(gatt.equals(bleGattLeft))
                {
                    Log.w(TAG, "bleGattLeft = " + bleGattLeft);
                    deviceNum=LEFT_BOOT;

                }else if(gatt.equals(bleGattRight))
                {
                    Log.w(TAG, "bleGattRight = " + bleGattRight);
                    deviceNum=RIGHT_BOOT;

                }else
                {
                    Log.e(TAG, "onServicesDiscovered received GATT_SUCCESS, but bleGattLeft and bleGattRight are not the discovered service");
                }
                createBroadcast(ACTION_GATT_SERVICES_DISCOVERED, deviceNum);

            } else
            {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "onCharacteristicRead()");
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
                //TODO: see if we need to add more here
                //probably not
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {

            if(dataTransmissionState == STATE_IDLE)
            {
                //We don't want to do anything with the data
                return;
            }
            Log.d(TAG, "onCharacteristicChanged(");
            byte[] receivedChar = characteristic.getValue();
            Log.d(TAG, "characteristic: "+receivedChar[0]+" "+receivedChar[1]+" "+receivedChar[2]+" "+receivedChar[3]+" "+receivedChar[4]+" "+receivedChar[5]+" "+receivedChar[6]+" "+receivedChar[7]+" "+receivedChar[8]);

            if(receivedChar[0] == (byte) 0x4C)
            {
                //data received from left BMH
                Log.d(TAG, "Received data from left BMH");
                for(int i=1;i<9;i++)
                {
                    leftBootRawData[i-1] = receivedChar[i];
                }
                leftBootDataReceived=true;

            }else if(receivedChar[0] == (byte) 0x52)
            {
                Log.d(TAG, "Received data from right BMH");
                for(int i=1;i<9;i++)
                {
                    rightBootRawData[i-1] = receivedChar[i];
                }
                rightBootDataReceived=true;

            }else
            {
                //Invalid header
                Log.e(TAG, "Invalid header code");
            }

            Log.d(TAG, "onCharacteristicChanged() completed correctly");

            //extractData(characteristic.getValue());

            //createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            Log.d(TAG, "onDescriptorWrite()");
            //check if both devices are ready, then send the order to start transmission

            if(bleGattLeft!=null)
            {
                if(bleGattRight != null)
                {
                    createBroadcast(ACTION_DEVICE_READY, RIGHT_BOOT);
                    Log.d(TAG, "starting data transfer");
                    startDataTransfer();//TODO: make sure this is in the right place, potentially put in individual activities
                }else
                {
                    //send broadcast for activity to prompt choice for second boot
                    createBroadcast(ACTION_DEVICE_READY, LEFT_BOOT);
                }
            }else if(bleGattRight != null)
            {
                //send broadcast for activity to prompt choice for second boot
                createBroadcast(ACTION_DEVICE_READY, RIGHT_BOOT);
            }
            else
            {
                Log.e(TAG, "Both Gatts are still null");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "onCharacteristicWrite()");
            byte[] sChar = characteristic.getValue();
            char char1 = (char) sChar[0];
            char char2 = (char) sChar[1];

            if(char1 == 'L')
            {
                if(char2 == 'L')
                {
                    Log.d(TAG, "Left boot now transmitting data");
                    if (rightBootTransmitting)
                    {
                        //both boots are transmitting data
                        Log.d(TAG, "Both boots now transmitting data");
                    } else
                    {
                        byte[] value = RIGHT_ENABLE.getBytes();//enable left boot
                        Log.d(TAG, "writeRXCharacteristic (value " + RIGHT_ENABLE + ", deviceNum RIGHT_BOOT)");
                        writeRXCharacteristic(value, RIGHT_BOOT);
                    }

                }else if(char2 == 'D')
                {
                    Log.d(TAG, "Left boot no longer transmitting data");
                    if (rightBootTransmitting)
                    {
                        //both boots are transmitting data
                        Log.d(TAG, "Right boot still transmitting data");
                        byte[] value = RIGHT_DISABLE.getBytes();//enable left boot
                        Log.d(TAG, "writeRXCharacteristic (value " + RIGHT_DISABLE + ", deviceNum RIGHT_BOOT)");
                        writeRXCharacteristic(value, RIGHT_BOOT);
                    } else
                    {
                        Log.d(TAG, "No boot still transmitting data");
                    }

                }else
                {
                    Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
                }
            }else if(char1 == 'R')
            {

                if(char2 == 'D')
                {
                    Log.d(TAG, "Right boot no longer transmitting data");
                    if (leftBootTransmitting)
                    {
                        //both boots are transmitting data
                        Log.d(TAG, "Left boot still transmitting data");
                        byte[] value = LEFT_DISABLE.getBytes();//enable left boot
                        Log.d(TAG, "writeRXCharacteristic (value " + LEFT_DISABLE + ", deviceNum RIGHT_BOOT)");
                        writeRXCharacteristic(value, LEFT_BOOT);
                    } else
                    {
                        Log.d(TAG, "No boot still transmitting data");
                    }
                }else if(char2 == 'E')
                {
                    Log.d(TAG, "Right boot now transmitting data");
                    if (leftBootTransmitting)
                    {
                        //both boots are transmitting data
                        Log.d(TAG, "Both boots now transmitting data");
                    } else
                    {
                        byte[] value = LEFT_ENABLE.getBytes();//enable left boot
                        Log.d(TAG, "writeRXCharacteristic (value " + LEFT_ENABLE + ", deviceNum RIGHT_BOOT)");
                        writeRXCharacteristic(value, LEFT_BOOT);
                    }

                }else
                {
                    Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
                }

            }else
            {
                Log.e(TAG, "Invalid characteristic written to a device: "+ char1 + " " + char2);
            }
        }
    };*/

    //Old stuff from before BLESvc merge
    /*
    private IntentFilter makeBLEIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FloeBLESvc.ACTION_DATA_AVAILABLE);
        return intentFilter;
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
            bleService=null;
        }
    };


    @Override
    public void onDestroy()
    {
        super.onDestroy();
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
        if(bleSvcBound && bleConnection!=null)
        {
            bleService.unbindService(bleConnection);
            bleSvcBound = false;
            bleService=null;
        }
    }

    //happens once: this service is bound to BLE service
    @Override
    public void onCreate()
    {
        Intent i = new Intent(this, FloeBLESvc.class);

        bindService(i, bleConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(BLEBroadcastReceiver, makeBLEIntentFilter());
    }

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

    private void createBroadcast(final String action, final int[] arrayOfData)
    {
        //This function sends out a broadcast with an int array of the sensor values or CoP, for the Calibrating or RTFeedback activity
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, arrayOfData);

        Log.d(TAG, "Sending broadcast " + action);
        //send out the broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createBroadcast(final String action, final FloeDataPt dataPt)
    {
        //This function sends out a broadcast with a dataPt object, for the recording activity
        Bundle b = new Bundle();
        b.putParcelable(BUNDLE_KEY, dataPt);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, b);

        Log.d(TAG, "Sending broadcast " + action);
        //send out the broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    */
}
