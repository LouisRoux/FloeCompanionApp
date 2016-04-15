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

public class FloeRTFeedbackAct extends AppCompatActivity {
    FloeDataTransmissionSvc dataService;
    boolean DTSvcBound = false;

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
/*
            int[] currentPoint = dataService.getCoP();
            if(currentPoint.length != 2)
            {
                Log.e("FloeRTFeedbackAct", "CoP array is incomplete");
            }
            x = currentPoint[0] + canvas.getWidth()/2 - 25;
            y = canvas.getHeight()/4 - currentPoint[1] - 25;
            if
*/
            //test thing
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
            //end test thing
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
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            DTSvcBound = false;
        }
    };
}