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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.UUID;

public class FloeBLESvc extends Service
{
    public static final String TAG = "FloeBLESvc";
    public final static String EXTRA_DATA = "com.pinnaclebiometrics.floecompanionapp.EXTRA_DATA";
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


    private final IBinder bleBinder = new FloeBLEBinder();
    private int connectionState = STATE_DISCONNECTED;

    //all bluetooth objects required to work properly
    private BluetoothManager bleManager = null;
    private BluetoothAdapter bleAdapter = null;
    private BluetoothDevice bleDevice1 = null;//maybe we don't need this here
    private String bleDevice1Address = null;
    private BluetoothGatt bleGatt1 = null;
    private BluetoothDevice bleDevice2 = null;//maybe we don't need this here
    private String bleDevice2Address = null;
    private BluetoothGatt bleGatt2 = null;

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

    @Override
    public boolean onUnbind(Intent intent)
    {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the dataTransmission Service is disconnected from the BLE Service.
        close(1);
        close(2);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate()
    {

    }

    @Override
    public void onDestroy()
    {

    }

    public class FloeBLEBinder extends Binder
    {
        FloeBLESvc getService()
        {
            return FloeBLESvc.this;
        }
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

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            int deviceNum=0;

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
            createBroadcast(ACTION_DATA_AVAILABLE, characteristic);
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
        switch (deviceNum)
        {
            case 1:
                BluetoothGattService RxService = bleGatt1.getService(RX_SERVICE_UUID);
                Log.e(TAG, "bleGatt1 null " + bleGatt1);
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                if (RxChar == null)
                {
                    Log.e(TAG,"Rx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                RxChar.setValue(value);
                boolean status = bleGatt1.writeCharacteristic(RxChar);

                Log.d(TAG, "write TXchar - status=" + status);
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
        switch(deviceNum)
        {
            case 1:
                BluetoothGattService RxService = bleGatt1.getService(RX_SERVICE_UUID);
                if (RxService == null)
                {
                    Log.e(TAG, "Rx service not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
                if (TxChar == null)
                {
                    Log.e(TAG, "Tx characteristic not found!");
                    createBroadcast(DEVICE_DOES_NOT_SUPPORT_UART, deviceNum);
                    return;
                }
                bleGatt1.setCharacteristicNotification(TxChar, true);

                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bleGatt1.writeDescriptor(descriptor);

                //check if both devices are ready, then send the order to start transmission
                if(bleGatt1!=null && bleGatt2 != null)
                {
                    //TODO: make sure this is in the right place
                    startDataTransfer();
                }

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

                //check if both devices are ready, then send the order to start transmission
                if(bleGatt1!=null && bleGatt2 != null)
                {
                    //TODO: make sure this is in the right place
                    startDataTransfer();
                }

                break;

            default:
                Log.e(TAG, "Invalid device number passed to enableTXNotification()");
                break;
        }
    }

    private void startDataTransfer()
    {
        //This function sends the expected values to tell the boards to start transmitting data
        byte[] value = "R00E00000".getBytes();//enable right boot
        Log.d(TAG, "writeRXCharacteristic (value R00E00000, deviceNum 1)");
        writeRXCharacteristic(value, 1);
        //TODO: uncomment following operations when second board is ready
        // value = "L00E00000".getBytes();//enable left boot
        writeRXCharacteristic(value, 2);
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

}