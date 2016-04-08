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
import com.pinnaclebiometrics.floecompanionapp.FloeDataTransmissionSvc.FloeDTBinder;

public class FloeRTFeedbackAct extends AppCompatActivity {

    FloeDataTransmissionSvc dataService;

    public class RTFeedbackView extends View {

        Bitmap ball;
        int x, y;

        public RTFeedbackView(Context context) {
            super(context);

            ball = BitmapFactory.decodeResource(getResources(), R.drawable.circle1);
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

            Paint p = new Paint();
            canvas.drawBitmap(ball, x, y, p);
            invalidate();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new RTFeedbackView(this));
        Intent i = new Intent(this, FloeDataTransmissionSvc.class);
        bindService(i, dataConnection, Context.BIND_AUTO_CREATE);

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
