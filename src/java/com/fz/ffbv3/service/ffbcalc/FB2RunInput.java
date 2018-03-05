/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FB2RunInput {
    
    public String hvsDate;
    public List<String> divList = new ArrayList<String>();
    public List<String> vehicleList = new ArrayList<String>();
    //public String runID;
    
    public int millEndTime = 21 * 60; // min
    public String millEndTimeStr = "21:00"; 
    
    public double binCapacity = 10000; // kg
    
    public int startFruitReadyForGrabber = 7 * 60; // min
    public String startFruitReadyForGrabberStr = "07:00"; // min
    
    public int durToFillBin = 30; // min
    public int speedKmPHr = 15; // km / hour
    public int durToLoadBinToVehicle = 15; // 
    public int durToWeight = 3;
    public int durToUnloadInMill = 3;
    public int durToUnloadInBlock = 10;
    public int durWaitingBridge = 3;
    
    public int maxIteration = 50;
    public String MultiDivPerVehicle = "Yes";
    public String orderByAlgo = "No";
    public String workFolder = "c:\\fza\\wrk\\";
    public String divIDListStr;
}
