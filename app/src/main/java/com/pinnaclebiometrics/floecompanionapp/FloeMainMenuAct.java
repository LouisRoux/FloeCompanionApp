package com.pinnaclebiometrics.floecompanionapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;

public class FloeMainMenuAct extends AppCompatActivity {

    FloeRunDatabase db = new FloeRunDatabase(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_floe_main_menu);

        Button calBtn = (Button) findViewById(R.id.calBtn);
        Button rtBtn = (Button) findViewById(R.id.rtBtn);
        Button recBtn = (Button) findViewById(R.id.recBtn);
        Button revBtn = (Button) findViewById(R.id.revBtn);

        final List<FloeRun> allRuns = db.getAllRuns();

        //test - adding run into database

        if(db.getAllRuns().size() < 1){
            FloeRun testRun = new FloeRun(0);
            testRun.setRunDuration(0);

            long runID = db.createRun(testRun);
            Log.w("FloeMainMenuAct","Database was empty, so new run with runID = "+runID+" was created to store weight. " +
                    "Database size now "+db.getAllRuns().size());

            Log.w("FloeMainMenuAct","Updating database with weight value now");
            int[] sensors = {100,0,0,0,0,0,0,0};
            int[] CoPs = {0,0};
            FloeDataPt weightPt = new FloeDataPt(1, 1, 0, sensors, CoPs);
            weightPt.setDataPtID(1);
            db.updateDataPt(weightPt);
        }

        //end test

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
                if (allRuns.size() < 1){
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_LONG).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else{
                    startActivity(new Intent(FloeMainMenuAct.this, FloeRTFeedbackAct.class));
                    Log.d("FloeMainMenuAct", "Realtime feedback activity started'");
                }
            }
        });

        recBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allRuns.size() < 1){
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_LONG).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else{
                    startActivity(new Intent(FloeMainMenuAct.this, FloeRecordingAct.class));
                    Log.d("FloeMainMenuAct", "Record activity started'");
                }
            }
        });

        revBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allRuns.size() < 1){
                    Toast.makeText(getApplicationContext(),
                            "Please calibrate weight first!", Toast.LENGTH_LONG).show();
                    Log.w("FloeMainMenuAct", "Database is empty; must calibrate weight first.");
                }
                else{
                    startActivity(new Intent(FloeMainMenuAct.this, FloeReviewListAct.class));
                    Log.d("FloeMainMenuAct", "Review stuff activity started'");
                }
            }
        });

    }

}
