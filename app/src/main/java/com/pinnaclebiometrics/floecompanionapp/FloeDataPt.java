package com.pinnaclebiometrics.floecompanionapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by Louis on 16/03/2016.
 */
public class FloeDataPt implements Parcelable
{
    private long dataPtID;//unique identifier for this data point
    private long runID;//what run this point belongs to
    private long dataPtNum;//where in the run this data point goes
    private long timeStamp;

    private int[] sensorData = new int[8];//size 8. starts at 0 for left foot, index increases as numbers in design specs fig 6.5
    private int[] centreOfPressure = new int[2];//array of size 2. x-dir is location 0, y-dir is location 1.

    FloeDataPt()
    {
        //empty constructor
    }


    FloeDataPt(long runID, int dataPtNum, int timeStamp, int[] sensorData, int[] centreOfPressure)
    {
        //TODO: implement this if we need it
    }


    FloeDataPt(long timeStamp, int[] sensorData, int[] centreOfPressure)
    {
        //create object using the provided sensor input. Make sure to check array sizes and throw error if appropriate
        if(sensorData.length==8 && centreOfPressure.length==2)
        {
            //setRunID(runID);
            //setDataPtNum(dataPtNum);
            setTimeStamp(timeStamp);
            setSensorData(sensorData);
            setCentreOfPressure(centreOfPressure);
        }
        else
        {
            Log.e("FloeDataPt", "The length of the sensorData or centreOfPressure array passed to the constructor was invalid");
        }
    }

    public FloeDataPt(Parcel in)
    {
        //This function used to build a DataPt object from a Parcel object.
        //dataPtID = in.readLong();
        //runID = in.readLong();
        //dataPtNum = in.readLong();
        timeStamp = in.readLong();

        sensorData = in.createIntArray();
        centreOfPressure = in.createIntArray();
    }

    //Functions that allow FloeDataPt to be Parcelable
    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        //This function used to create a Parcel object
        //dest.writeLong(dataPtID);
        //dest.writeLong(runID);
        //dest.writeLong(dataPtNum);
        dest.writeLong(timeStamp);

        dest.writeIntArray(sensorData);
        dest.writeIntArray(centreOfPressure);
    }

    public static final Parcelable.Creator<FloeDataPt> CREATOR = new Parcelable.Creator<FloeDataPt>() 
    {
        public FloeDataPt createFromParcel(Parcel in) 
        {
            return new FloeDataPt(in);
        }

        public FloeDataPt[] newArray(int size) 
        {
            return new FloeDataPt[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }


    //Setters and getters for the attributes of FloeDataPt
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
