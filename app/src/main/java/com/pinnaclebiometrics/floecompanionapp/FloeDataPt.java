package com.pinnaclebiometrics.floecompanionapp;

import android.util.Log;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeDataPt
{
    private long dataPtID;//unique identifier for this data point
    private long runID;//what run this point belongs to
    private long dataPtNum;//where in the run this data point goes
    private long timeStamp;
    private int[] sensorData;//size 8. starts at 0 for left foot, index increases as numbers in design specs fig 6.5
    private int[] centreOfPressure;//array of size 2. x-dir is location 0, y-dir is location 1.

    FloeDataPt()
    {
        //empty constructor
    }

    FloeDataPt(int runID, int dataPtNum, int timeStamp, int[] sensorData, int[] centreOfPressure)
    {
        //create object using the provided sensor input. Make sure to check array sizes and throw error if appropriate
        if(sensorData.length==8 && centreOfPressure.length==2)
        {
            setRunID(runID);
            setDataPtNum(dataPtNum);
            setTimeStamp(timeStamp);
            setSensorData(sensorData);
            setCentreOfPressure(centreOfPressure);
        }
        else
        {
            Log.e("INVALID_ARRAY_LENGTH", "The length of the sensorData or centreOfPressure array passed to the FloeDataPt constructor was invalid");
        }
    }

    public long getDataPtID()
    {
        return dataPtID;
    }

    public void setDataPtID(long dataPtID)
    {
        this.dataPtID = dataPtID;
    }


    public long getRunID()
    {
        return runID;
    }

    public void setRunID(long runID)
    {
        this.runID = runID;
    }


    public long getDataPtNum()
    {
        return dataPtNum;
    }

    public void setDataPtNum(long dataPtNum)
    {
        this.dataPtNum = dataPtNum;
    }


    public long getTimeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }


    public int[] getSensorData()
    {
        return sensorData;
    }

    public int getSensorData(int sensorNum)
    {//do we need this method?
        return sensorData[sensorNum];
    }

    public void setSensorData(int[] sensorData)
    {
        for(int i=0; i<8; i++)
        {
            this.sensorData[i] = sensorData[i];
        }
    }

    public void setSensorData(int sensorNum, int value)
    {//do we need this method?
        this.sensorData[sensorNum]=value;
    }

    public int[] getCentreOfPressure()
    {
        return centreOfPressure;
    }

    public int getCentreOfPressure(int direction)
    {//do we need this method?
        return centreOfPressure[direction];
    }

    public void setCentreOfPressure(int[] centreOfPressure)
    {
        for(int i=0; i<2; i++)
        {
            this.centreOfPressure[i] = centreOfPressure[i];
        }
    }

    public void setCentreOfPressure(int direction, int value)
    {//do we need this method?
        this.centreOfPressure[direction]=value;
    }

}
