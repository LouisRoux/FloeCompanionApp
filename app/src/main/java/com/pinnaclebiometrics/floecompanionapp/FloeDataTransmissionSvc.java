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

//TODO: merge with BLESvc
//TODO: Change broadcast receivers to 'get' functions?

public class FloeDataTransmissionSvc extends Service
{
    public static final String TAG = "FloeDataTransmissionSvc";
    public static final String BUNDLE_KEY = "bundled_Data_Pt";
    public final static String EXTRA_DATA = "com.pinnaclebiometrics.floecompanionapp.EXTRA_DATA";
    public final static String NEW_DATA_PT_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE";
    public final static String NEW_DATA_PT_AVAILABLE_NR = "com.pinnaclebiometrics.floecompanionapp.NEW_DATA_PT_AVAILABLE_NR";
    public final static String NEW_COP_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_COP_AVAILABLE";
    public final static String NEW_SENSOR_DATA_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.NEW_SENSOR_DATA_AVAILABLE";
    public static final int STATE_IDLE = 0;
    public static final int STATE_RT_FEEDBACK = 10;
    public static final int STATE_RECORDING = 20;
    public static final int STATE_CALIBRATING = 30;

    public final static String ACTION_GATT_CONNECTED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.pinnaclebiometrics.floecompanionapp.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.pinnaclebiometrics.floecompanionapp.ACTION_DATA_AVAILABLE";
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
    private static int dataTransmissionState = 0;

    private boolean newRun = true;
    private int[] sensorData = new int[8];//array to store sensor data temporarily
    private int[] centreOfPressure = new int[2]; //array to store calculated CoP value

    private boolean leftBootDataReceived = false;
    private boolean rightBootDataReceived = false;

    //all bluetooth objects required to work properly
    private BluetoothManager bleManager = null;
    private BluetoothAdapter bleAdapter = null;
    private BluetoothDevice bleDevice1 = null;//maybe we don't need this here
    private String bleDevice1Address = null;
    private BluetoothGatt bleGatt1 = null;
    private BluetoothDevice bleDevice2 = null;//maybe we don't need this here
    private String bleDevice2Address = null;
    private BluetoothGatt bleGatt2 = null;

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
    public int[] getCoP()
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

        Log.d(TAG, "Bx="+dBx+" By="+dBy+"M5x="+dM5x+" M5y="+dM5y+"M1x="+dM1x+" M1y="+dM1y+"Hx="+dHx+" Hy="+dHy);
        Log.d(TAG, "CoPx = "+CoPx+" , CoPy = "+CoPy);

        int[] CoP = {CoPx, CoPy};
        return CoP;
    }

    private void extractData(final byte[] txValue)
    {
        //TODO: verify the data-extracting function works
        Log.d(TAG, "extractData()");
        byte[] dataBytes = {0,0,0,0};
        int sensorValue;
        int baseIndex=0;//baseIndex tells us where to write the data in the sensorData array

        if(txValue[0] == (byte) 0x4C)
        {
            //data received from left BMH
            if(leftBootDataReceived)
            {
                Log.d(TAG, "Left boot data already received");
                return;
            }
            baseIndex=1;
            Log.d(TAG, "Received data from left BMH");
        }else if(txValue[0] == (byte) 0x52)
        {
            //Data received from right BMH
            if(rightBootDataReceived)
            {
                Log.d(TAG, "Right boot data already received");
            }
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
            sensorData[baseIndex+j-1] = Linearize(sensorValue);
            Log.i(TAG, "Unpacked data from sensor " + (baseIndex+j-1) + ". value = " + sensorValue);
        }

        //check if we have enough data to create a DataPt object.
        if(leftBootDataReceived && rightBootDataReceived)
        {
            Log.d(TAG, "Received data from both boots");
            //We now have data from both boots, so we decide what to do next based on the activity
            switch (dataTransmissionState)
            {
                case STATE_RT_FEEDBACK:
                    Log.d(TAG, "performing operation for STATE_RT_FEEDBACK");
                    getCoP();
                    //send out broadcast using NEW_COP_AVAILABLE
                    createBroadcast(NEW_COP_AVAILABLE, centreOfPressure);
                    break;
                case STATE_RECORDING:
                    Log.d(TAG, "performing operation for STATE_RECORDING");
                    getCoP();
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
            leftBootDataReceived = false;
            rightBootDataReceived = false;
        }
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
    }

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback()
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

                if(gatt.equals(bleGatt1))
                {
                    deviceNum=1;
                    if(bleGatt2!=null)
                    {
                        connectionState = STATE_2_CONNECTED;
                    }else
                    {
                        connectionState = STATE_1_CONNECTED;
                    }
                    createBroadcast(intentAction, deviceNum);
                    Log.i(TAG, "Connected to GATT server 1.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + bleGatt1.discoverServices());

                }else if(gatt.equals(bleGatt2))
                {
                    deviceNum=2;
                    if(bleGatt1!=null)
                    {
                        connectionState = STATE_2_CONNECTED;
                    }else
                    {
                        connectionState = STATE_1_CONNECTED;
                    }
                    createBroadcast(intentAction, deviceNum);
                    Log.i(TAG, "Connected to GATT server 2.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + bleGatt2.discoverServices());

                }else
                {
                    //somehow the connected gatt is neither device 1 nor 2
                    Log.e(TAG, "Connected to GATT Server that is not bleGatt1 nor bleGatt2");
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                intentAction = ACTION_GATT_DISCONNECTED;

                if(bleGatt1==null && bleGatt2==null)
                {
                    connectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from last GATT server.");

                    switch(FloeDataTransmissionSvc.getDataTransmissionState())
                    {
                        //TODO: modify RTFeedback and Calibration Activities to include isDeviceConnected(), then un-comment
                        /*case FloeDataTransmissionSvc.STATE_RT_FEEDBACK:
                            if(FloeRTFeedbackAct.isDeviceConnected(1))
                            {
                                deviceNum=1;
                            }else if(FloeRTFeedbackAct.isDeviceConnected(2))
                            {
                                deviceNum=2;
                            }
                            break;*/

                        /*case FloeDataTransmissionSvc.STATE_RECORDING:
                            if(FloeRecordingAct.isDeviceConnected(1))
                            {
                                deviceNum=1;
                            }else if(FloeRecordingAct.isDeviceConnected(2))
                            {
                                deviceNum=2;
                            }
                            break;*/

                        /*case FloeDataTransmissionSvc.STATE_CALIBRATING:
                            if(FloeCalibrationAct.isDeviceConnected(1))
                            {
                                deviceNum=1;
                            }else if(FloeCalibrationAct.isDeviceConnected(2))
                            {
                                deviceNum=2;
                            }
                            break;*/

                        default:
                            break;
                    }

                }else if(bleGatt1!=null)
                {
                    connectionState = STATE_1_CONNECTED;
                    deviceNum=2;
                    Log.i(TAG, "Disconnected from GATT server 2.");

                }else if(bleGatt2!=null)
                {
                    connectionState = STATE_1_CONNECTED;
                    deviceNum=1;
                    Log.i(TAG, "Disconnected from GATT server 1.");

                }else
                {
                    Log.e(TAG, "Disconnected from a GATT server, but bleGatt1 and bleGatt2 still not null");
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
                if(gatt.equals(bleGatt1))
                {
                    Log.w(TAG, "bleGatt1 = " + bleGatt1);
                    deviceNum=1;

                }else if(gatt.equals(bleGatt2))
                {
                    Log.w(TAG, "bleGatt2 = " + bleGatt2);
                    deviceNum=2;

                }else
                {
                    Log.e(TAG, "onServicesDiscovered received GATT_SUCCESS, but bleGatt1 and bleGatt2 are not the discovered service");
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
                //probably not since dataTransmissionSvc does all the data unpacking
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            Log.d(TAG, "onCharacteristicChanged()");
            createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            Log.d(TAG, "onDescriptorWrite()");
            //check if both devices are ready, then send the order to start transmission
            if(bleGatt1!=null /*&& bleGatt2 != null*/)
            {
                //TODO: uncomment conditional field
                //TODO: make sure this is in the right place
                Log.d(TAG, "starting data transfer");
                startDataTransfer();
            }
        }
    };

    public boolean initialize()
    {
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
                if (bleDevice1Address != null && address.equals(bleDevice1Address) && bleGatt1 != null)
                {
                    Log.d(TAG, "Trying to use an existing bleGatt1 for connection.");
                    if (bleGatt1.connect())
                    {
                        connectionState = STATE_CONNECTING;
                        return true;

                    } else
                    {
                        return false;
                    }
                }

                // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
                Log.d(TAG, "Trying to create a new connection for bleGatt1.");
                bleGatt1 = device.connectGatt(this, false, bleGattCallback);
                Log.d(TAG, "bleGatt1 = " + bleGatt1);
                bleDevice1Address = address;
                Log.d(TAG, "bleDevice1Address = " + bleDevice1Address);
                connectionState = STATE_CONNECTING;
                return true;

            case 2:

                // Previously connected device.  Try to reconnect.
                if (bleDevice2Address != null && address.equals(bleDevice2Address) && bleGatt2 != null)
                {
                    Log.d(TAG, "Trying to use an existing bleGatt2 for connection.");
                    if (bleGatt2.connect())
                    {
                        connectionState = STATE_CONNECTING;
                        return true;

                    } else
                    {
                        return false;
                    }
                }

                // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
                bleGatt2 = device.connectGatt(this, false, bleGattCallback);
                Log.d(TAG, "Trying to create a new connection for bleGatt2.");
                bleDevice2Address = address;
                connectionState = STATE_CONNECTING;
                return true;

            default:
                Log.e(TAG, "Invalid device number passed to connect()");
                return false;
        }
    }

    public void disconnect(int deviceNum)
    {
        switch (deviceNum)
        {
            case 1:
                if (bleAdapter == null || bleGatt1 == null)
                {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                    return;
                }
                bleGatt1.disconnect();
                break;
            case 2:
                if (bleAdapter == null || bleGatt2 == null)
                {
                    Log.w(TAG, "BluetoothAdapter not initialized");
                    return;
                }
                bleGatt2.disconnect();
                break;
            default:
                Log.e(TAG, "Invalid device number passed to disconnect()");
                break;
        }
    }

    public void close(int deviceNum)
    {
        switch(deviceNum)
        {
            case 1:
                if (bleGatt1 == null)
                {
                    return;
                }
                Log.w(TAG, "bleGatt1 closed");
                bleDevice1Address = null;
                bleGatt1.close();
                bleGatt1 = null;
                break;

            case 2:
                if (bleGatt2 == null)
                {
                    return;
                }
                Log.w(TAG, "bleGatt1 closed");
                bleDevice2Address = null;
                bleGatt2.close();
                bleGatt2 = null;
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
            case 1:
                BluetoothGattService RxService = bleGatt1.getService(RX_SERVICE_UUID);
                Log.e(TAG, "bleGatt1 null " + bleGatt1);
                Log.d(TAG, "bleGatt1 RxService = " + RxService.toString());
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                Log.d(TAG, "RxChar = "+RxChar.toString());
                if (RxChar == null)
                {
                    Log.e(TAG,"Rx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar.setValue(value);
                boolean status = bleGatt1.writeCharacteristic(RxChar);

                Log.d(TAG, "write TXchar - status = " + status);
                break;

            case 2:
                RxService = bleGatt2.getService(RX_SERVICE_UUID);
                Log.e(TAG, "bleGatt2 null" + bleGatt1);
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                if (RxChar == null)
                {
                    Log.e(TAG,"Rx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar.setValue(value);
                status = bleGatt2.writeCharacteristic(RxChar);

                Log.d(TAG, "write TXchar - status=" + status);
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
            case 1:
                BluetoothGattService RxService = bleGatt1.getService(RX_SERVICE_UUID);
                Log.d(TAG, "RxService = "+RxService.toString());
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
                Log.d(TAG, "set bleGatt1 Characteristic Notification " + TxChar.toString());
                bleGatt1.setCharacteristicNotification(TxChar, true);

                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                Log.d(TAG, "writing bleGatt1 descriptor " + descriptor);
                bleGatt1.writeDescriptor(descriptor);
                //writeDescriptor() will call bleGattCallback.onDescriptorWrite() upon successful descriptor write

                break;

            case 2:
                RxService = bleGatt2.getService(RX_SERVICE_UUID);
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
                bleGatt2.setCharacteristicNotification(TxChar,true);

                descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bleGatt2.writeDescriptor(descriptor);

                break;

            default:
                Log.e(TAG, "Invalid device number passed to enableTXNotification()");
                break;
        }
    }

    private void startDataTransfer()
    {
        //This function sends the expected values to tell the boards to start transmitting data
        Log.d(TAG, "startDataTransfer()");
        byte[] value = "RS".getBytes();//enable right boot
        Log.d(TAG, "writeRXCharacteristic (value RS, deviceNum 1)");
        writeRXCharacteristic(value, 1);
        //TODO: uncomment following operations when second board is ready
        // value = "LS".getBytes();//enable left boot
        //writeRXCharacteristic(value, 2);
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
            case 1:
                if (bleGatt1 == null)
                {
                    Log.w(TAG, "bleGatt1 not initialized");
                    return;
                }
                bleGatt1.readCharacteristic(characteristic);
                break;

            case 2:
                if (bleGatt2 == null)
                {
                    Log.w(TAG, "bleGatt2 not initialized");
                    return;
                }
                bleGatt2.readCharacteristic(characteristic);
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
        if(bleGatt1!=null)
        {
            close(1);
        }
        if(bleGatt2!=null)
        {
            close(2);
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
    */
}
