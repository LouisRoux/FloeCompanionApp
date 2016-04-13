package com.pinnaclebiometrics.floecompanionapp;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeRun
{
    private long runID;
    private long runTime;
    private int runDuration;
    private String runName;
    //Do we need another attribute with the location of beginning of this run's data in the dataPt table?

    FloeRun()
    {
        //empty constructor
    }

    FloeRun(long runTime)
    {
        //initialize object with provided data. generate name from timestamp, wait until end of run for duration
        //setRunID(runID);
        setRunTime(runTime);
        setRunName(generateRunName());
        setRunDuration(-1);
    }

    public long getRunID()
    {
        return runID;
    }

    public void setRunID(long runID)
    {
        this.runID = runID;
    }


    public long getRunTime()
    {
        return runTime;
    }

    public void setRunTime(long runTime)
    {
        this.runTime = runTime;
    }


    public int getRunDuration()
    {
        return runDuration;
    }

    public void setRunDuration(int runDuration)
    {
        this.runDuration = runDuration;
    }


    public String getRunName()
    {
        return runName;
    }

    public void setRunName(String runName)
    {
        this.runName = runName;
    }


    public String generateRunName()
    {
        //generate a run name from the timestamp
        String newName = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(runTime));
        //todo verify that generateRunName outputs the correct name

        return newName;
    }
}