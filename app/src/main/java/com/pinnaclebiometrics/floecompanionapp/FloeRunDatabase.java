package com.pinnaclebiometrics.floecompanionapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeRunDatabase extends SQLiteOpenHelper
{
    //Database name and version
    private static final String DATABASE_NAME="floeRunDatabase";
    private static final int DATABASE_VERSION=1;

    //Table Names
    private static final String TABLE_RUNS = "runs";
    private static final String TABLE_DATA_PTS = "data_Pts";

    //Common Column Names
    private static final String KEY_RUN_ID = "run_ID";

    //Runs Table Column Names
    private static final String KEY_RUN_TIME = "run_Time";
    private static final String KEY_RUN_DURATION = "run_Duration";
    private static final String KEY_RUN_NAME = "run_Name";

    //DataPts Table Column Names
    private static final String KEY_DATA_PT_ID = "dataPt_ID";
    private static final String KEY_DATA_PT_NUM = "dataPt_Num";
    private static final String KEY_TIMESTAMP = "timeStamp";
    private static final String KEY_SENSOR_0 = "sensor_0";
    private static final String KEY_SENSOR_1 = "sensor_1";
    private static final String KEY_SENSOR_2 = "sensor_2";
    private static final String KEY_SENSOR_3 = "sensor_3";
    private static final String KEY_SENSOR_4 = "sensor_4";
    private static final String KEY_SENSOR_5 = "sensor_5";
    private static final String KEY_SENSOR_6 = "sensor_6";
    private static final String KEY_SENSOR_7 = "sensor_7";
    private static final String KEY_COP_X = "centre_Of_Pressure_X";
    private static final String KEY_COP_Y = "centre_Of_Pressure_Y";

    //Runs Table create statement. Do we really use DATETIME datatype for run time? or maybe just integer and convert on read? or maybe just text?
    private static final String CREATE_TABLE_RUNS = "CREATE TABLE "+TABLE_RUNS+" ("+KEY_RUN_ID+" INTEGER PRIMARY KEY, "+KEY_RUN_TIME+" DATETIME, "+KEY_RUN_DURATION+" INTEGER, "+KEY_RUN_NAME+" TEXT)";

    //DataPts Table create statement. Do we really use DATETIME datatype for time stamps? or maybe just integer?
    private static final String CREATE_TABLE_DATA_PTS = "CREATE TABLE "+TABLE_DATA_PTS+" ("+KEY_DATA_PT_ID+" INTEGER PRIMARY KEY, "+KEY_RUN_ID+" INTEGER, "+KEY_DATA_PT_NUM+" INTEGER, "+KEY_TIMESTAMP+" DATETIME, "+KEY_SENSOR_0+" REAL, "+KEY_SENSOR_1+" REAL, "+KEY_SENSOR_2+" REAL, "+KEY_SENSOR_3+" REAL, "+KEY_SENSOR_4+" REAL, "+KEY_SENSOR_5+" REAL, "+KEY_SENSOR_6+" REAL, "+KEY_SENSOR_7+" REAL, "+KEY_COP_X+" REAL, "+KEY_COP_Y+" REAL)";

    FloeRunDatabase(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase dataBase)
    {
        //Create tables
        dataBase.execSQL(CREATE_TABLE_RUNS);
        dataBase.execSQL(CREATE_TABLE_DATA_PTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase dataBase, int oldVersion, int newVersion)
    {
        //drop older tables
        dataBase.execSQL("DROP TABLE IF EXISTS "+TABLE_RUNS);
        dataBase.execSQL("DROP TABLE IF EXISTS "+TABLE_DATA_PTS);
        //create new tables
        onCreate(dataBase);
    }

    public int createRun(FloeRun newRun)
    {
        //create new entry in Runs table
        int runID;
        return runID;//ok
    }

    public FloeRun getRun(int runID)
    {
        //returns a run object to use to display menu in FloeReviewListAct
        FloeRun run = new FloeRun();
        return run;
    }

    public List<FloeRun> getAllRuns()
    {
        //returns a list of all runs in database
        List<FloeRun> allRuns = new ArrayList<FloeRun>();
        return allRuns;
    }

    public void deleteRun(int runID)
    {
        //deletes the run whose ID is passed, including all associated data points
    }

    public int updateRun(int runID, FloeRun run)
    {
        //updates the run data in the database from the passed run object
    }

    
    public int createDataPt(FloeDataPt dataPt)
    {
        //creates new entry in DataPt table
    }

    public FloeDataPt getDataPt(int dataPtID)
    {
        //returns the specified data point object
    }

    public List<FloeDataPt> getRunDataPts(int runID)
    {
        //returns a list of all dataPts from a given run
    }

    public void deleteDataPt(int dataPtID)
    {
        //deletes a data point
    }

    public int updateDataPt(int dataPtID, FloeDataPt dataPt)
    {
        //updates the specified table entry with data from dataPt object
    }
}
