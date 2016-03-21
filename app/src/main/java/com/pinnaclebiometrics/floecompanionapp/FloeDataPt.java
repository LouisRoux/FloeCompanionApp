package com.pinnaclebiometrics.floecompanionapp;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeDataPt
{
    private int runID;
    private int dataPtNum;
    private int timeStamp;
    private double[] sensorData;//size 8. starts at 0 for left foot, index increases as numbers in design specs fig 6.5
    private double[] centreOfPressure;//array of size 2. x-dir is location 0, y-dir is location 1.

    FloeDataPt()
    {
        //create empty object with all invalid values for attributes? or nothing?
        /*
        this.runID=-1;
        this.dataPtNum=-1;
        this.timeStamp=-1;
        for(int i=0; i<8; i++)
        {
            this.sensorData[i]=-1;
        }
        this.centreOfPressure[0]=0;
        this.centreOfPressure[1]=0;
        */
    }

    FloeDataPt(int runID, int dataPtNum, int timeStamp, double[] sensorData, double[] centreOfPressure)
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
            //Throw error?
        }
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


    public double[] getSensorData()
    {
        return sensorData;
    }

    public double getSensorData(int sensorNum)
    {//do we need this method?
        return sensorData[sensorNum];
    }

    public void setSensorData(double[] sensorData)
    {
        this.sensorData = sensorData;
    }

    public void setSensorData(int sensorNum, double value)
    {//do we need this method?
        this.sensorData[sensorNum]=value;
    }

    public double[] getCentreOfPressure()
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

}
