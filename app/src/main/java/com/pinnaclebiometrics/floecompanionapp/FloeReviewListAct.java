package com.pinnaclebiometrics.floecompanionapp;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FloeReviewListAct extends AppCompatActivity {

    FloeRunDatabase db = new FloeRunDatabase(this);
    ArrayList<String> runs = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_review_list);
//test
        //db.deleteRun(1);

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
        //end test
        getRuns();
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
            String runName = currentRun.getRunName();
            runs.add(runName);
        }
        ListView reviewList = (ListView) findViewById(R.id.reviewList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,runs);

        if (adapter != null){
            reviewList.setAdapter(adapter);
            Log.e("ReviewListAct", "Adapter is set!");
        }
        else{
            Log.e("ReviewListAct", "Adapter was null");
        }


    }

}
