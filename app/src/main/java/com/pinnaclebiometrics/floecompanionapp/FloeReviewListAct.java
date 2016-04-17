package com.pinnaclebiometrics.floecompanionapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class FloeReviewListAct extends AppCompatActivity {

    FloeRunDatabase db = new FloeRunDatabase(this);
    ArrayList<FloeRun> runs = new ArrayList<FloeRun>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floe_review_list);
//test - inserting new run
        FloeRun testRun = new FloeRun(1000000000);
        testRun.setRunDuration(0);
        testRun.setRunName("yumyumrun#"+db.getAllRuns().size());

        long runID = db.createRun(testRun);

        Log.w("FloeReviewListAct", "testRun added to db");
        Log.w("FloeReviewListAct", db.getRun(runID).getRunName()+" added to db");

        for (int i = 0; i < 50; i++){
            int j[] = {-i,i};
            int k[] = {1, 1, 1, 1, 1, 1, 1, 1};
            FloeDataPt testPt = new FloeDataPt(i, k, j);
            testPt.setRunID(runID);
            db.createDataPt(testPt);
            Log.w("FloeReviewListAct", "testPt "+i+" added to db");
        }
//end test
        getRuns();
        Log.w("FloeReviewListAct", "end of onCreate");
    }

    private void getRuns() {
        Log.w("FloeReviewListAct", "in getRuns fxn now!");
        List<FloeRun> allRuns = db.getAllRuns();
        Log.w("FloeReviewListAct","allRuns size is "+allRuns.size());

        if (allRuns.size() == 1){
            Toast.makeText(getApplicationContext(),
                    "No recorded runs!", Toast.LENGTH_LONG).show();
            Log.w("FloeReviewListAct", "No recorded runs to display.");
            return;
        }

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
        runs.remove(0);
        ArrayAdapter<FloeRun> adapter = new ArrayAdapter<FloeRun>(this, android.R.layout.simple_list_item_1,runs);
        final ListView reviewList = (ListView) findViewById(R.id.reviewList);

        if (adapter != null){
            reviewList.setAdapter(adapter);
            Log.e("ReviewListAct", "Adapter is set!");
        }
        else{
            Log.e("ReviewListAct", "Adapter was null");
        }

        reviewList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FloeRun selectedRun = ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).getItem(position);
                long runID = selectedRun.getRunID();

                Intent intent = new Intent(FloeReviewListAct.this, FloeRunReviewAct.class);
                intent.putExtra("runID", runID);
                startActivity(intent);
            }
        });

        reviewList.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(FloeReviewListAct.this);
                alertDialogBuilder.setMessage("Selected run: " + ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).getItem(position).getRunName());

                alertDialogBuilder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.cancel();
                    }
                });

                alertDialogBuilder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        FloeRun selectedRun = ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).getItem(position);
                        final long runID = selectedRun.getRunID();


                        final AlertDialog.Builder alertDialogBuilder1 = new AlertDialog.Builder(FloeReviewListAct.this);
                        alertDialogBuilder1.setMessage("Please enter the new name:");

                        final EditText input = new EditText(FloeReviewListAct.this);
                        input.setId(0);
                        alertDialogBuilder1.setView(input);

                        alertDialogBuilder1.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                arg0.cancel();
                            }
                        });

                        alertDialogBuilder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                String newName = input.getText().toString();

                                db.updateRunName(runID, newName);
                                ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).clear();
                                ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).notifyDataSetChanged();
                                Log.w("FloeReviewListAct","Adapter has been cleared and set.");
                                Log.w("FloeReviewListAct","Now calling getRuns to update list.");

                                getRuns();
                            }
                        });

                        AlertDialog alertDialog = alertDialogBuilder1.create();
                        alertDialog.show();

                    }
                });

                alertDialogBuilder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        FloeRun selectedRun = ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).getItem(position);
                        long runID = selectedRun.getRunID();
                        db.deleteRun(runID);
                        ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).clear();
                        ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).notifyDataSetChanged();
                        Log.w("FloeReviewListAct", "Adapter has been cleared and set.");
                        Log.w("FloeReviewListAct", "Now calling getRuns to update list.");

                        getRuns();
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            }
        });

        Log.w("FloeReviewListAct", "getRuns finished.");

        final Button deleteAll = (Button) findViewById(R.id.deleteAll);

        deleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.deleteAll();
                ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).clear();
                ((ArrayAdapter<FloeRun>) reviewList.getAdapter()).notifyDataSetChanged();
                getRuns();
            }
        });
    }



}