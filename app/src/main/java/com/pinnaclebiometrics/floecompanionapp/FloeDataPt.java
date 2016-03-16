package com.pinnaclebiometrics.floecompanionapp;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeDataPt
{
    private int runID;
    private int dataPtNum;
    private int timeStamp;
    private int[] sensorData;//size 8. starts at 0 for left foot, index increases as numbers in design specs fig 6.5
    private double[] centreOfPressure;//array of size 2. x-dir is location 0, y-dir is location 1.

    FloeDataPt()
    {
        //create empty object with all null values for attributes
    }

    FloeDataPt(int[] inputData)
    {
        //create object using the provided sensor input. Make sure to check sensor count and throw error if appropriate

    }

    public int getRunID()
    {
        return runID;
    }

    public void setRunID(int runID)
    {
        this.runID = runID;
    }


    public int getDataPtNum()
    {
        return dataPtNum;
    }

    public void setDataPtNum(int dataPtNum)
    {
        this.dataPtNum = dataPtNum;
    }


    public int getTimeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp)
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
        this.sensorData = sensorData;
    }

    public void setSensorData(int sensorNum, int value)
    {//do we need this method?
        this.sensorData[sensorNum]=value;
    }


    public int[] getCentreOfPressure()
    {
        return centreOfPressure;
    }

    public double getCentreOfPressure(int direction)
    {//do we need this method?
        return centreOfPressure[direction];
    }

    public void setCentreOfPressure(double[] centreOfPressure)
    {
        this.centreOfPressure=centreOfPressure;
    }

    public void setCentreOfPressure(int direction, double value)
    {//do we need this method?
        this.centreOfPressure[direction]=value;
    }

    public void calcCoP()
    {
        
    }


}
