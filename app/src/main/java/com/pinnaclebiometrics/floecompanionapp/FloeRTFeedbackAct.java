package com.pinnaclebiometrics.floecompanionapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.view.WindowManager;

import com.pinnaclebiometrics.floecompanionapp.FloeDataTransmissionSvc.FloeDTBinder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.Double;

public class FloeRTFeedbackAct extends AppCompatActivity {
    public static final String TAG = "FloeRTFeedbackAct";
    FloeDataTransmissionSvc dataService;
    boolean DTSvcBound = false;
    int prevXValue;
    int prevYValue;

    public class RTFeedbackView extends View {
        Bitmap ball;
        Bitmap logo;
        int x, y;
        public RTFeedbackView(Context context) {
            super(context);
            ball = BitmapFactory.decodeResource(getResources(), R.drawable.circle1);
            logo = BitmapFactory.decodeResource(getResources(), R.drawable.pblogo);
            x = 0;
            y = 0;
        }
        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            Rect rectScreen = new Rect();
            rectScreen.set(0, 0, canvas.getWidth(), canvas.getHeight() / 2);
            Paint black = new Paint();
            black.setColor(Color.BLACK);
            black.setStyle(Paint.Style.FILL);
            canvas.drawRect(rectScreen, black);

            byte[][] rawData = dataService.getRawData();
            if(rawData!= null)
            {
                int[] sensorData = extractData(rawData);
                int[] currentPoint = getCoP(sensorData);
                if (currentPoint.length != 2)
                {
                    Log.e("FloeRTFeedbackAct", "CoP array is incomplete");
                }
                x = currentPoint[0] + canvas.getWidth() / 2 - 25;
                prevXValue=x;
                y = canvas.getHeight() / 4 - currentPoint[1] - 25;
                prevYValue=y;
            }else
            {
                x=prevXValue;
                y=prevYValue;
            }


            /*//test thing
            Log.v("FloeRTFeedbackAct", "canvas width = "+canvas.getWidth());
            Log.v("FloeRTFeedbackAct", "canvas height = "+canvas.getHeight());
            if (x < canvas.getWidth() - 40){
                x+=5;
            }
            else {
                x=0;
            }
            if (y < canvas.getHeight()/2 - 40){
                y+=5;
            }
            else{
                y=0;
            }
            //end test thing*/


            Paint red = new Paint();
            red.setColor(Color.RED);
            red.setStyle(Paint.Style.STROKE);
            red.setStrokeWidth(8);
            Paint yellow = new Paint();
            yellow.setColor(Color.YELLOW);
            yellow.setStyle(Paint.Style.STROKE);
            yellow.setStrokeWidth(3);
            if(x < 270) {
                canvas.drawLine(150,0,150,canvas.getHeight()/2,yellow);
            }
            if(x > 770) {
                canvas.drawLine(930,0,930,canvas.getHeight()/2,yellow);
            }
            if(y < 222) {
                canvas.drawLine(0,126,1080,126,yellow);
            }
            if(y > 626) {
                canvas.drawLine(0,762,1080,762,yellow);
            }
            if(x < 135) {
                canvas.drawLine(135,0,135,canvas.getHeight()/2,red);
            }
            if(x > 905) {
                canvas.drawLine(945,0,945,canvas.getHeight()/2,red);
            }
            if(y < 111) {
                canvas.drawLine(0,111,1080,111,red);
            }
            if(y > 737) {
                canvas.drawLine(0,777,1080,777,red);
            }
            int cx = (canvas.getWidth() - logo.getWidth()) >> 1;
            canvas.drawBitmap(logo, cx, 950, null);
            Paint p = new Paint();
            canvas.drawBitmap(ball, x, y, p);
            invalidate();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(new RTFeedbackView(this));
        doBindService();

    }
    public void doBindService(){
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);
        DTSvcBound = true;
        Log.w("RTFeedback", "dataTransSvc bound!");
    }
    @Override
    public void onDestroy()
    {
        //This makes sure to unbind the bleSvc to avoid leaking a ServiceConnection
        //TODO: make sure every bound service gets unbound when its client stops
        dataService.stopDataTransfer();
        super.onDestroy();
        if (DTSvcBound && dataConnection != null)
        {
            unbindService(dataConnection);
            Log.w("RTFeedback", "dataTransSvc unbound!");
            DTSvcBound = false;
        }
    }
    private ServiceConnection dataConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloeDataTransmissionSvc.FloeDTBinder binder = (FloeDataTransmissionSvc.FloeDTBinder) service;
            dataService = binder.getService();
            DTSvcBound = true;
            Log.w("RTFeedback","dataTransSvc bound in onServiceConnected!");
            dataService.startDataTransfer();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            DTSvcBound = false;
        }
    };

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
        //FloeDataPt temp = db.getDataPt(1);
        //int weight = temp.getSensorData(1);
        int weight = 10000;

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

        for (int k = 0; k < 2; k++)
        {
            for (int j = 0; j < 8; j += 2)
            {
                sensorValue = ByteBuffer.wrap(rawData[k]).order(ByteOrder.LITTLE_ENDIAN).getShort(j);//TODO: verify data
                Log.i(TAG, "Unpacked data from sensor " + ((k * 4) + (j / 2)) + ". value = " + sensorValue);
                sensorData[(k * 4) + (j / 2)] = Linearize(sensorValue);
                Log.i(TAG, "Linearized data from sensor " + ((k * 4) + (j / 2)) + ". value = " + sensorData[(k * 4) + (j / 2)]);
            }
        }
        return sensorData;
    }

    public int Linearize(int v)
    {
        /*if(v<1)
        {
            v=1;
        }
        Log.d(TAG, "Linearize("+v+")");
        double r2 = 10000;
        //TODO: input voltage value?
        double inputVoltage = 4095;
        double exponent = 1/0.9;

        Double temp = Math.pow((inputVoltage/v - 1)*r2, exponent);
        return temp.intValue();*/
        return v;//TODO: linearize properly
    }
}