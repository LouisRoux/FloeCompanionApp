package com.pinnaclebiometrics.floecompanionapp;

import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class FloeRunReviewAct extends AppCompatActivity {

    private LineGraphSeries<DataPoint> series;

    public void displayGraphs(){
        GraphView graph1 = (GraphView) findViewById(R.id.graph1);
        series = new LineGraphSeries<DataPoint>();
        graph1.addSeries(series);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_run_review);

        displayGraphs();
    }

}
