/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

/**
 *
 * @author Eri Fizal
 */
public class FB2Job {
    
    public FB2Context cx;
    public FB2Vehicle vehicle = null;
    public FB2Demand demand = null;
    
//    public int startTime = -1;
//    public int endTime = -1;
//    
//    public int timeArriveAtDemand = -1;
//    public int durWaitingDemand = -1;
//    public int timeDepartToMill = -1;
//    public int timeArriveAtMill = -1;
//    
    public FB2Location startLoc = null;
    public FB2Location endLoc = null;
    public FB2JobTiming jobTiming = new FB2JobTiming();
    
    public int jobIDInDb = -1;
    
    public FB2Job(FB2Context cx, FB2Vehicle v, FB2Demand d
            , int startTime, FB2Location startLoc) throws Exception {
        this.cx = cx;
        this.vehicle = v;
        this.demand = d;
        this.jobTiming.startTime = startTime;
        this.startLoc = startLoc;
        //this.calcTimeAndLoc(startTime, startLoc);
    }

    public String getID(){
        String vID = "";
        if (this.vehicle != null) vID = this.vehicle.vehicleID;
        
        String dID = "";
        if (this.demand != null) dID = this.demand.demandID;
        
        return vID + "_" + dID;
    }
    
    private FB2Trip findPath(FB2Context cx
            , FB2Location from1, FB2Location to1) throws Exception {
        
        // find path in global var
        String pathID  =  FB2Trip.buildID(from1, to1);
        FB2Trip p = null;
        for (FB2Trip ip : cx.trips){
            if (ip.getID().equals(pathID)){
                p = ip;
                break;
            }
        }
        // if not found
        if (p == null){
            
            // create
            p = new FB2Trip(cx, from1, to1);
            
            // add to global var for reuse
            cx.trips.add(p);
        }
        
        return p;
    }
    
    public void calcTimeAndLoc(int newStartTime, FB2Location startLoc1) 
            throws Exception {
        
        // determine locations
        startLoc = startLoc1;//.cloneIt();
        endLoc = demand.division.millLoc; //.cloneIt();
        
        // determine times
        jobTiming.calcTime(newStartTime
                , cx.runInput
                , demand.dueTime
                , startLoc.lon
                , startLoc.lat
                , demand.loc.lon
                , demand.loc.lat
        );
        
//        jobTiming.startTime = newStartTime;
//        
//        // calc trip to demand
//        FB2Trip pathToDemand = findPath(cx
//                , startLoc //.cloneIt()
//                , demand.loc //.cloneIt()
//        );
//        jobTiming.timeArriveAtDemand = this.jobTiming.startTime 
//                + pathToDemand.getTripMinutes()
//                + cx.runInput.durToUnloadInBlock
//                ;
//        
//        // calc waiting demand
//        jobTiming.durWaitingDemand = demand.dueTime - jobTiming.timeArriveAtDemand;
//        if (jobTiming.durWaitingDemand < 0) jobTiming.durWaitingDemand = 0;
//        
//        // calc depart to mill
//        jobTiming.timeDepartToMill = this.jobTiming.timeArriveAtDemand 
//                + jobTiming.durWaitingDemand
//                + cx.runInput.durToLoadBinToVehicle;
//        
//        // calc trip to mill
//        FB2Trip pathToMill = findPath(cx
//                , demand.loc //.cloneIt()
//                , demand.division.millLoc //.cloneIt()
//        );
//        jobTiming.timeArriveAtMill = jobTiming.timeDepartToMill + pathToMill.getTripMinutes();
//        
//        // calc end time
//        jobTiming.endTime = jobTiming.timeArriveAtMill 
//                + cx.runInput.durWaitingBridge
//                + cx.runInput.durToWeight
//                + cx.runInput.durToUnloadInMill
//                ;
        
    }
    
    public boolean isFeasible() {
        int millEndTime = cx.runInput.millEndTime;
        int curEndTime = this.jobTiming.endTime;
        boolean feas = curEndTime < millEndTime;
        
        return feas;
    }

    FB2Job cloneIt() throws Exception {
        FB2Job j = new FB2Job(cx, vehicle, demand, jobTiming.startTime, startLoc);
        j.calcTimeAndLoc(jobTiming.startTime, startLoc);
        return j;
    }
    
    public boolean sameWith(FB2Job j){
        boolean same = false; 
        if (this.vehicle.vehicleID.equals(j.vehicle.vehicleID)){
            if (this.demand.demandID.equals(j.demand.demandID)){
                same = true;
            }
        }
        return same;
    }
}
