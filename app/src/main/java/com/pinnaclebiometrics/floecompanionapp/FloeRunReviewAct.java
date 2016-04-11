package com.pinnaclebiometrics.floecompanionapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.content.DialogInterface;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import java.util.List;

public class FloeRunReviewAct extends AppCompatActivity {

    private LineGraphSeries<DataPoint> series;
    double lastX = 0;
    FloeRunDatabase db;
    final Handler handler = new Handler();

    public void displayGraphs(){
        GraphView graph1 = (GraphView) findViewById(R.id.graph1);
        series = new LineGraphSeries<DataPoint>();
        addEntries();
        graph1.addSeries(series);

    }

    private void addEntries(){
        //TODO: add runID
        Log.w("FloeRunReviewAct","it got to addEntries!");

        //adding "Please wait" dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                finish();
            }
        });
        builder.setMessage("Please wait; graphs generating :)");

        final AlertDialog dialog = builder.create();
        dialog.show();

        //thread to get run from db
        final Thread t = new Thread() {
            @Override
            public void run() {
                //test stuff
                db.deleteRun(1);

                FloeRun testRun = new FloeRun(1, 1);
                testRun.setRunDuration(0);
                db.createRun(testRun);
                Log.w("FloeRunReviewAct", "testRun added to db");

                for (int i = 0; i < 300; i++){
                    int j[] = {-i,i};
                    Log.w("FloeRunReviewAct", "j array initiated");
                    int k[] = {1, 1, 1, 1, 1, 1, 1, 1};
                    Log.w("FloeRunReviewAct", "k array initiated");
                    FloeDataPt testPt = new FloeDataPt(testRun.getRunID(),i,i, k, j);
                    db.createDataPt(testPt);
                    Log.w("FloeRunReviewAct", "testPt "+i+" added to db");
                }

                //end test stuff

                //TODO: change parameter in getRunDataPts
                List<FloeDataPt> currentRun = db.getRunDataPts(1);

                for (int i = 0; i < currentRun.size(); i++){
                    FloeDataPt currentPt = currentRun.get(i);
                    final double CoPx = (double) currentPt.getCentreOfPressure(0);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            series.appendData(new DataPoint(lastX++, CoPx), false, 1000000000);
                        }
                    });
                }
                dialog.dismiss();
            }
        };

        t.start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_run_review);
        Log.w("FloeRunReviewAct", "got to onCreate!");
        db = new FloeRunDatabase(getApplicationContext());
        displayGraphs();

    }

}
