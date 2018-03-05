/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.ffbcalc;

import com.fz.util.FZUtil;

/**
 *
 */
public class FB2JobTiming {

    public int startTime = -1;
    public int endTime = -1;
    
    public int timeArriveAtDemand = -1;
    public int durWaitingDemand = -1;
    public int timeDepartToMill = -1;
    public int timeArriveAtMill = -1;
    public int tripMin = -1;

    public void calcTime(int startTime
            , FB2RunInput runInput
            , int demandDue
            , double lon1
            , double lat1
            , double lon2
            , double lat2
    ){

        this.startTime = startTime;
        
        // calc trip min. Symetric to and from
        double distMtr = FZUtil.calcMeterDist(
                lon1
                , lat1
                , lon2
                , lat2
        );
        tripMin = FZUtil.calcTripMinutes(distMtr
                , runInput.speedKmPHr);
        tripMin = (int) Math.floor(((double) tripMin) * 1.1);
        
        // calc time arrive at demand
        this.timeArriveAtDemand = this.startTime 
                + tripMin
                + runInput.durToUnloadInBlock
                ;
        
        // calc waiting demand
        this.durWaitingDemand = demandDue - this.timeArriveAtDemand;
        if (this.durWaitingDemand < 0) this.durWaitingDemand = 0;
        
        // calc depart to mill
        this.timeDepartToMill = this.timeArriveAtDemand 
                + this.durWaitingDemand
                + runInput.durToLoadBinToVehicle;
        
        // calc arrive at mill
        this.timeArriveAtMill = this.timeDepartToMill + tripMin;
        
        // calc end time
        this.endTime = this.timeArriveAtMill 
                + runInput.durWaitingBridge
                + runInput.durToWeight
                + runInput.durToUnloadInMill
                ;
    }
    
}
