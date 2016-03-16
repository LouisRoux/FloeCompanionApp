package com.pinnaclebiometrics.floecompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class FloeMainMenuAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_main_menu);

        Button calBtn = (Button) findViewById(R.id.calBtn);
        Button rtBtn = (Button) findViewById(R.id.rtBtn);
        Button recBtn = (Button) findViewById(R.id.recBtn);
        Button revBtn = (Button) findViewById(R.id.revBtn);

        calBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this,FloeCalibrationAct.class));
            }
        });

        rtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeRTFeedbackAct.class));
            }
        });

        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeRecordingAct.class));
            }
        });

        revBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FloeMainMenuAct.this, FloeReviewListAct.class));
            }
        });

    }

}
