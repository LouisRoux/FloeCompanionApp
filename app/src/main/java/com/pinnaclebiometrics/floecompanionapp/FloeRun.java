package com.pinnaclebiometrics.floecompanionapp;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeRun
{
    private long runID;
    private long runTime;
    private int runDuration;
    private String runName;
    //Do we need another attribute with the location of beginning of this run's data in the datPt table?

    FloeRun()
    {
        //initialize object with invalid attributes? or nothing
    }

    FloeRun(int runID, int runTime)
    {
        //initialize object with provided data. generate name from timestamp, wait until end of run for duration
        setRunID(runID);
        setRunTime(runTime);
        setRunName(generateRunName());
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
        String newName="";
        //generate a run name from the timestamp
        //todo write name generator

        return newName;
    }
}
