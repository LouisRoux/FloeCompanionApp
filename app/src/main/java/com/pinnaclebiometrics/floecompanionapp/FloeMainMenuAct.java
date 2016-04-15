package com.pinnaclebiometrics.floecompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.WindowManager;

public class FloeMainMenuAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_floe_main_menu);

        Button calBtn = (Button) findViewById(R.id.calBtn);
        Button rtBtn = (Button) findViewById(R.id.rtBtn);
        Button recBtn = (Button) findViewById(R.id.recBtn);
        Button revBtn = (Button) findViewById(R.id.revBtn);


        calBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this,FloeCalibrationAct.class));
                Log.d("FloeMainMenuAct", "Calibration Activity started'");

            }
        });

        rtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeRTFeedbackAct.class));
                Log.d("FloeMainMenuAct", "Realtime feedback activity started'");
            }
        });

        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeRecordingAct.class));
                Log.d("FloeMainMenuAct", "Record activity started'");
            }
        });

        revBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeReviewListAct.class));
                Log.d("FloeMainMenuAct", "Review stuff activity started'");
            }
        });

    }

}
