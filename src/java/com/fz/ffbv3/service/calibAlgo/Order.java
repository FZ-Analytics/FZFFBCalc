/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.calibAlgo;

import com.fz.ffbv3.service.ffbcalc.FB2Job;
import com.fz.ffbv3.service.ffbcalc.FB2JobTiming;
import com.fz.ffbv3.service.ffbcalc.FB2Location;
import com.fz.ffbv3.service.ffbcalc.FB2RunInput;

/**
 *
 */
public class Order {
    
    public String jobID;
    public String divID;
    public String betweenBlock1;
    public String betweenBlock2;
    public int readyTime;
    public String readyTimeStr;
    public double estmKg;
    public FB2Location millLoc;
    public FB2Location block1Loc;
//    public int startTime;
//    public int endTime;
//    public int timeArriveAtDemand;
//    public int timeDepartToMill;
//    public int tripMillToBlock;
    public FB2JobTiming jobTiming = new FB2JobTiming();
    public FB2RunInput runInput;
    public String runID;
    public String isLastJob;
}
