package com.pinnaclebiometrics.floecompanionapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FloeReviewListAct extends AppCompatActivity {

    FloeRunDatabase db = new FloeRunDatabase(this);
    ArrayList<FloeRun> runs = new ArrayList<FloeRun>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_review_list);
//test

/*
        List<FloeRun> allRuns = db.getAllRuns();
        for (int i = 0; i < allRuns.size(); i++){
            FloeRun currentRun = allRuns.get(i);
            long runID2beDeleted = currentRun.getRunID();
            db.deleteRun(runID2beDeleted);
            Log.w("FloeReviewListAct","RunID = " +runID2beDeleted+" has been deleted");
        }
*/
        FloeRun testRun = new FloeRun(1000000000);
        testRun.setRunDuration(0);

        long runID = db.createRun(testRun);

        Log.w("FloeRunReviewAct", "testRun added to db");

        for (int i = 0; i < 50; i++){
            int j[] = {-i,i};
            int k[] = {1, 1, 1, 1, 1, 1, 1, 1};
            FloeDataPt testPt = new FloeDataPt(i, k, j);
            testPt.setRunID(runID);
            db.createDataPt(testPt);
            Log.w("FloeRunReviewAct", "testPt "+i+" added to db");
        }
        //end test
        getRuns();
        Log.w("FloeReviewListAct","end of onCreate");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_floe_main_menu, menu);
        db = new FloeRunDatabase(getApplicationContext());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getRuns() {
        List<FloeRun> allRuns = db.getAllRuns();
        for (int i = 0; i < allRuns.size(); i++){
            FloeRun currentRun = allRuns.get(i);
            if (currentRun != null && currentRun.getRunName() != null){
                runs.add(currentRun);
            }
            else{
                currentRun.setRunName("NULL NAME");
            }
            Log.w("FloeReviewListAct"," " +runs.get(i)+" is added at i = "+i);
        }
        final ListView reviewList = (ListView) findViewById(R.id.reviewList);
        final ArrayAdapter<FloeRun> adapter = new ArrayAdapter<FloeRun>(this, android.R.layout.simple_list_item_1,runs);

        if (adapter != null){
            reviewList.setAdapter(adapter);
            Log.e("ReviewListAct", "Adapter is set!");
        }
        else{
            Log.e("ReviewListAct", "Adapter was null");
        }

        reviewList.setOnItemClickListener( new OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FloeRun selectedRun = adapter.getItem(position);
                long runID = selectedRun.getRunID();

                Intent intent = new Intent(FloeReviewListAct.this, FloeRunReviewAct.class);
                intent.putExtra("runID", runID);
                startActivity(intent);
            }
            });

        Log.w("FloeReviewListAct", "getRuns finished.");

    }



}