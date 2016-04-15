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

    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;
    double lastX = 0;
    double lastY = 0;
    FloeRunDatabase db;
    final Handler handler = new Handler();

    public void displayGraphs(){
        GraphView graph1 = (GraphView) findViewById(R.id.graph1);
        GraphView graph2 = (GraphView) findViewById(R.id.graph2);
        series1 = new LineGraphSeries<DataPoint>();
        series2 = new LineGraphSeries<DataPoint>();
        addEntries();
        graph1.addSeries(series1);
        graph2.addSeries(series2);
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
/*
                FloeRun testRun = new FloeRun(1);
                testRun.setRunDuration(1);
                testRun.setRunName("Marshmallow");
                long testRunID = db.createRun(testRun);
                Log.w("FloeRunReviewAct", "testRun "+testRunID+ " added to db");

                for (int i = 0; i < 100; i++){
                    int j[] = {-i,i};
                    int k[] = {1, 1, 1, 1, 1, 1, 1, 1};
                    FloeDataPt testPt = new FloeDataPt(i, k, j);
                    testPt.setRunID(testRunID);
                    db.createDataPt(testPt);
                    Log.w("FloeRunReviewAct", "testPt "+i+" added to db");
                }
*/
                //end test stuff

                long runID;
                Bundle extras = getIntent().getExtras();
                if(extras == null) {
                    runID = 0;
                } else {
                    runID= extras.getLong("runID");
                }

                //TODO: change parameter in getRunDataPts
                List<FloeDataPt> currentRun = db.getRunDataPts(runID);

                if (currentRun.size() == 0){
                    Log.e("FloeRunReviewAct","No data points in this run to graph!");
                }

                for (int i = 0; i < currentRun.size(); i++){
                    FloeDataPt currentPt = currentRun.get(i);
                    final double CoPx = (double) currentPt.getCentreOfPressure(0);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            series2.appendData(new DataPoint(lastX++, CoPx), false, 100000);
                        }
                    });
                    Log.w("FloeRunReview","Data point "+i+" = "+currentPt.getCentreOfPressure(0)+" was added to the series.");
                }

                for (int i = 0; i < currentRun.size(); i++){
                    FloeDataPt currentPt = currentRun.get(i);
                    final double CoPy = (double) currentPt.getCentreOfPressure(1);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            series1.appendData(new DataPoint(lastY++, CoPy), false, 100000);
                        }
                    });
                    Log.w("FloeRunReview","Data point "+i+" = "+currentPt.getCentreOfPressure(0)+" was added to the series.");
                }

                dialog.dismiss();
                Log.w("RunReviewAct", "series on graph ended with " + series1.getHighestValueX());
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
