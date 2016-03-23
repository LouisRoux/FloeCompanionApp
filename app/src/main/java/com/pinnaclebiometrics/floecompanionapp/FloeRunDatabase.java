package com.pinnaclebiometrics.floecompanionapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

    //Runs Table create statement
    private static final String CREATE_TABLE_RUNS = "CREATE TABLE "+TABLE_RUNS+" ("+KEY_RUN_ID+" INTEGER PRIMARY KEY, "+KEY_RUN_TIME+" INTEGER, "+KEY_RUN_DURATION+" INTEGER, "+KEY_RUN_NAME+" TEXT)";

    //DataPts Table create statement
    private static final String CREATE_TABLE_DATA_PTS = "CREATE TABLE "+TABLE_DATA_PTS+" ("+KEY_DATA_PT_ID+" INTEGER PRIMARY KEY, "+KEY_RUN_ID+" INTEGER, "+KEY_DATA_PT_NUM+" INTEGER, "+KEY_TIMESTAMP+" INTEGER, "+KEY_SENSOR_0+" REAL, "+KEY_SENSOR_1+" REAL, "+KEY_SENSOR_2+" REAL, "+KEY_SENSOR_3+" REAL, "+KEY_SENSOR_4+" REAL, "+KEY_SENSOR_5+" REAL, "+KEY_SENSOR_6+" REAL, "+KEY_SENSOR_7+" REAL, "+KEY_COP_X+" REAL, "+KEY_COP_Y+" REAL)";


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
        dataBase.execSQL("DROP TABLE IF EXISTS " + TABLE_RUNS);
        dataBase.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_PTS);
        //create new tables
        onCreate(dataBase);
    }


    public long createRun(FloeRun newRun)
    {
        //create new entry in Runs table
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_RUN_ID, newRun.getRunID());
        values.put(KEY_RUN_TIME, newRun.getRunTime());
        values.put(KEY_RUN_NAME, newRun.getRunName());
        values.put(KEY_RUN_DURATION, newRun.getRunDuration());

        long runID = db.insert(TABLE_RUNS, null, values);

        return runID;
    }

    public FloeRun getRun(long runID)
    {
        //returns a run object to use to display menu in FloeReviewListAct

        SQLiteDatabase db = this.getReadableDatabase();
        FloeRun run = new FloeRun();

        String selectQuery = "SELECT  * FROM "+TABLE_RUNS+" WHERE "+KEY_RUN_ID+" = "+runID;

        Log.e("DATABASE_QUERY_SENT", selectQuery);

        Cursor curs = db.rawQuery(selectQuery, null);

        if (curs != null)
        {
            curs.moveToFirst();
        }

        run.setRunID(curs.getLong(curs.getColumnIndex(KEY_RUN_ID)));
        run.setRunTime((curs.getLong(curs.getColumnIndex(KEY_RUN_TIME))));
        run.setRunName(curs.getString(curs.getColumnIndex(KEY_RUN_NAME)));
        run.setRunDuration(curs.getInt(curs.getColumnIndex(KEY_RUN_DURATION)));

        return run;
    }

    public List<FloeRun> getAllRuns()
    {
        //returns a list of all runs in database
        SQLiteDatabase db = this.getReadableDatabase();
        List<FloeRun> allRuns = new ArrayList<FloeRun>();
        String selectQuery = "SELECT  * FROM " + TABLE_RUNS;

        Log.e("DATABASE_QUERY_SENT", selectQuery);

        Cursor curs = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (curs.moveToFirst())
        {
            do
            {
                FloeRun run = new FloeRun();
                run.setRunID(curs.getLong((curs.getColumnIndex(KEY_RUN_ID))));
                run.setRunTime((curs.getLong(curs.getColumnIndex(KEY_RUN_TIME))));
                run.setRunName(curs.getString(curs.getColumnIndex(KEY_RUN_NAME)));
                run.setRunDuration(curs.getInt(curs.getColumnIndex(KEY_RUN_DURATION)));

                // adding to Run list
                allRuns.add(run);

            } while (curs.moveToNext());
        }
        return allRuns;
    }

    public void deleteRun(long runID)
    {
        //deletes the run whose ID is passed, including all associated data points
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  * FROM "+TABLE_DATA_PTS+" WHERE "+KEY_RUN_ID+" = "+runID;

        Log.e("DATABASE_QUERY_SENT", selectQuery);

        Cursor curs = db.rawQuery(selectQuery, null);

        //looping trough all rows and deleting data points
        if(curs.moveToFirst())
        {
            do
            {
                long dataPtID = curs.getLong(curs.getColumnIndex(KEY_RUN_ID));
                db.delete(TABLE_DATA_PTS, KEY_DATA_PT_ID + " = ?", new String[] {String.valueOf(dataPtID)});
            }while (curs.moveToNext());
        }

        db.delete(TABLE_RUNS, KEY_RUN_ID + " = ?", new String[]{String.valueOf(runID)});

        //todo verify that deleteRun deletes all data points for the run and not others
    }

    public int updateRun(FloeRun run)
    {
        //updates the run data in the database from the passed run object
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_RUN_ID, run.getRunID());
        values.put(KEY_RUN_TIME, run.getRunTime());
        values.put(KEY_RUN_NAME, run.getRunName());
        values.put(KEY_RUN_DURATION, run.getRunDuration());

        return db.update(TABLE_RUNS, values, KEY_RUN_ID+" = ?", new String[] {String.valueOf(run.getRunID())});
    }

    
    public long createDataPt(FloeDataPt dataPt)
    {
        //creates new entry in DataPt table
        SQLiteDatabase db = this.getWritableDatabase();
        long dataPtID;

        ContentValues values = new ContentValues();
        values.put(KEY_DATA_PT_ID, dataPt.getDataPtID());
        values.put(KEY_RUN_ID, dataPt.getRunID());
        values.put(KEY_DATA_PT_NUM, dataPt.getDataPtNum());
        values.put(KEY_TIMESTAMP, dataPt.getTimeStamp());
        values.put(KEY_SENSOR_0, dataPt.getSensorData(0));
        values.put(KEY_SENSOR_1, dataPt.getSensorData(1));
        values.put(KEY_SENSOR_2, dataPt.getSensorData(2));
        values.put(KEY_SENSOR_3, dataPt.getSensorData(3));
        values.put(KEY_SENSOR_4, dataPt.getSensorData(4));
        values.put(KEY_SENSOR_5, dataPt.getSensorData(5));
        values.put(KEY_SENSOR_6, dataPt.getSensorData(6));
        values.put(KEY_SENSOR_7, dataPt.getSensorData(7));
        values.put(KEY_COP_X, dataPt.getCentreOfPressure(0));
        values.put(KEY_COP_Y, dataPt.getCentreOfPressure(1));

        dataPtID = db.insert(TABLE_DATA_PTS, null, values);
        return dataPtID;
    }

    public FloeDataPt getDataPt(long dataPtID)
    {
        //returns the specified data point object
        SQLiteDatabase db = this.getReadableDatabase();
        FloeDataPt dataPt = new FloeDataPt();

        String selectQuery = "SELECT  * FROM "+TABLE_DATA_PTS+" WHERE "+KEY_DATA_PT_ID+" = "+dataPtID;
        Cursor curs = db.rawQuery(selectQuery, null);

        if(curs != null)
        {
            curs.moveToFirst();
        }

        dataPt.setDataPtID(curs.getLong(curs.getColumnIndex(KEY_DATA_PT_ID)));
        dataPt.setRunID(curs.getLong(curs.getColumnIndex(KEY_RUN_ID)));
        dataPt.setDataPtNum(curs.getLong(curs.getColumnIndex(KEY_DATA_PT_NUM)));
        dataPt.setTimeStamp(curs.getLong(curs.getColumnIndex(KEY_TIMESTAMP)));
        dataPt.setSensorData(0,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_0)));
        dataPt.setSensorData(1,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_1)));
        dataPt.setSensorData(2,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_2)));
        dataPt.setSensorData(3,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_3)));
        dataPt.setSensorData(4,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_4)));
        dataPt.setSensorData(5,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_5)));
        dataPt.setSensorData(6,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_6)));
        dataPt.setSensorData(7,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_7)));
        dataPt.setCentreOfPressure(0,curs.getDouble(curs.getColumnIndex(KEY_COP_X)));
        dataPt.setCentreOfPressure(1,curs.getDouble(curs.getColumnIndex(KEY_COP_Y)));

        return dataPt;
    }

    public List<FloeDataPt> getRunDataPts(long runID)
    {
        //returns a list of all dataPts from a given run
        SQLiteDatabase db = this.getReadableDatabase();
        List<FloeDataPt> dataPts = new ArrayList<FloeDataPt>();

        // The following statement stores into selectQuery the following string: SELECT * FROM data_Pts tdp, runs tr WHERE tdp.run_ID = ‘[runID]’ AND tr.run_ID = tdp.run_id
        String selectQuery = "SELECT * FROM "+TABLE_DATA_PTS+" tdp, "+TABLE_RUNS+" tr WHERE tdp."+KEY_RUN_ID+" = /'"+runID+"/' AND tr."+KEY_RUN_ID+" = tdp."+KEY_RUN_ID;

        Log.e("DATABASE_QUERY_SENT", selectQuery);

        Cursor curs = db.rawQuery(selectQuery, null);

        //looping trough all rows and adding to list
        if(curs.moveToFirst())
        {
            do
            {
                FloeDataPt dataPt = new FloeDataPt();

                dataPt.setDataPtID(curs.getLong(curs.getColumnIndex(KEY_DATA_PT_ID)));
                dataPt.setRunID(curs.getLong(curs.getColumnIndex(KEY_RUN_ID)));
                dataPt.setDataPtNum(curs.getLong(curs.getColumnIndex(KEY_DATA_PT_NUM)));
                dataPt.setTimeStamp(curs.getLong(curs.getColumnIndex(KEY_TIMESTAMP)));
                dataPt.setSensorData(0, curs.getDouble(curs.getColumnIndex(KEY_SENSOR_0)));
                dataPt.setSensorData(1,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_1)));
                dataPt.setSensorData(2,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_2)));
                dataPt.setSensorData(3,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_3)));
                dataPt.setSensorData(4,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_4)));
                dataPt.setSensorData(5,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_5)));
                dataPt.setSensorData(6,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_6)));
                dataPt.setSensorData(7,curs.getDouble(curs.getColumnIndex(KEY_SENSOR_7)));
                dataPt.setCentreOfPressure(0, curs.getDouble(curs.getColumnIndex(KEY_COP_X)));
                dataPt.setCentreOfPressure(1,curs.getDouble(curs.getColumnIndex(KEY_COP_Y)));

                dataPts.add(dataPt);
            }while (curs.moveToNext());
        }

        return dataPts;
        //todo verify that getRunDataPts properly returns all dataPts from a given run
    }

    public void deleteDataPt(long dataPtID)
    {
        //deletes a data point
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DATA_PTS, KEY_DATA_PT_ID+" = ?", new String[] {String.valueOf(dataPtID)});
    }

    public int updateDataPt(FloeDataPt dataPt)
    {
        //updates the specified table entry with data from dataPt object
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_DATA_PT_ID, dataPt.getDataPtID());
        values.put(KEY_RUN_ID, dataPt.getRunID());
        values.put(KEY_DATA_PT_NUM, dataPt.getDataPtNum());
        values.put(KEY_TIMESTAMP, dataPt.getTimeStamp());
        values.put(KEY_SENSOR_0, dataPt.getSensorData(0));
        values.put(KEY_SENSOR_1, dataPt.getSensorData(1));
        values.put(KEY_SENSOR_2, dataPt.getSensorData(2));
        values.put(KEY_SENSOR_3, dataPt.getSensorData(3));
        values.put(KEY_SENSOR_4, dataPt.getSensorData(4));
        values.put(KEY_SENSOR_5, dataPt.getSensorData(5));
        values.put(KEY_SENSOR_6, dataPt.getSensorData(6));
        values.put(KEY_SENSOR_7, dataPt.getSensorData(7));
        values.put(KEY_COP_X, dataPt.getCentreOfPressure(0));
        values.put(KEY_COP_Y, dataPt.getCentreOfPressure(1));

        return db.update(TABLE_DATA_PTS, values, KEY_RUN_ID+" = ?", new String[] {String.valueOf(dataPt.getDataPtID())});
    }
}
