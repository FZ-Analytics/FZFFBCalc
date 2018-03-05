/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FTwist;
import com.fz.ffbv3.service.optialg.FRestrictList;
import com.fz.ffbv3.service.optialg.FSolution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Eri Fizal
 */
public class FB2Solution extends FSolution {

    public FB2Context cx = null;
        
    
    private ArrayList<FB2Job> jobs 
            = new  ArrayList<FB2Job>();

    public ArrayList<FB2Job> getJobs() {
        return jobs;
    }

    public FB2Solution(FB2Context cx){
        this.cx = cx;
    } 
    
    public FB2Vehicle get1UnassignedVehicle() {
        
        // for each vehicle
        for (FB2Vehicle v : cx.vehicles){
            
            // for each pickup tasks
            boolean assigned = false;
            for (FB2Job p : this.jobs) {
                
                // if vehicle is assigned
                if (p.vehicle == v){
                    // break
                    assigned = true;
                    break;
                }
            }
            if (!assigned) 
                return v;
        }
        return null;
    }

    public void addJob(FB2Job a) {
        jobs.add(a);
    }

    void recalcTimes() throws Exception {
        (new FB2TwistImpactCalculator()).calc(cx, this);
    }

    public ArrayList<FB2Job> getVehicleJobs(FB2Vehicle vehicle) {
        
        ArrayList<FB2Job> jobs = new ArrayList<FB2Job>();
        for (FB2Job j : this.getJobs()){
            if (j.vehicle.vehicleID.equals(vehicle.vehicleID)){
                jobs.add(j);
            }
        }
        return jobs;
    }
    
    public double calcUnhandledSize() {
        
        double unhandledSize = 0;
        unhandledSize = cx.totalDemandSize - this.calcHandledSize();
        if (unhandledSize < 0){
            this.calcHandledSize();
        }
        return unhandledSize;
    }

    public double calcHandledSize() {
        
        double handledSize = 0;
        
        // for each jobs
        for (FB2Job j : this.getJobs()){
            
            // if job is feasible
            if (j.isFeasible()){
                
                // add as handled
                handledSize += j.demand.size;
            }
        }
        return handledSize;
    }

    public int calcNumberOfUsedVehicle() {
        
        int numOfUsedVehicle = 0;
        
        // for each vehicle
        for (FB2Vehicle v : cx.vehicles){
            
            // get jobs
            ArrayList<FB2Job> vj = this.getVehicleJobs(v);
            
            // if jobs count > 0
            if (vj.size() > 0){
                
                // add to used
                numOfUsedVehicle++;
            }
        }
        return numOfUsedVehicle;
    }
    
    public int calcTotalTime() {
        int totalTime = 0;
        // for each jobs
        for (FB2Job j : this.getJobs()){
            // if job is feasible
            if (j.isFeasible()){
                // add (endTime - startTime) as totalTrip
                totalTime += j.jobTiming.endTime - j.jobTiming.startTime;
            }
        }
        return totalTime;
    }
    
    public int calcMaxEndTime() {
        int maxEndTime = 0;
        // for each jobs
        for (FB2Job j : this.getJobs()){
//            if (j.isFeasible()){
                if (j.jobTiming.endTime > maxEndTime){
                    maxEndTime = j.jobTiming.endTime;
                }
//            }
        }
        return maxEndTime;
    }
    
    @Override
    public FSolution cloneIt() throws Exception {
        FB2Solution sol1 = new FB2Solution(cx);
        
        // copy jobs
        for (FB2Job j : this.getJobs()){
            FB2Job j2 = j.cloneIt();
            sol1.jobs.add(j2);
        }
        
        return sol1;
    }

    public FB2Job removeJob(String jobID1) {
        FB2Job fj = null;
        for (FB2Job j : this.getJobs()){
            if (j.getID().equals(jobID1)){
                fj = j;
                getJobs().remove(j);
                break;
            }
        }
        return fj;
    }
    
    public FB2Job getJob(String jobID1) {
        for (FB2Job j : this.getJobs()){
            if (j.getID().equals(jobID1))
                return j;
        }
        return null;
    }

    @Override
    protected void cleanUp() {
        for (FB2Job j : jobs){
            j.demand = null;
            j.vehicle = null;
            j.startLoc = null;
            j.endLoc = null;
            j = null;
        }
        jobs.clear();
        jobs = null;
    }

    boolean isDemandAssigned(FB2Demand d) {
        for (FB2Job j : this.getJobs()){
            if (j.demand.demandID.equals(d.demandID)) return true;
        }
        return false;
    }

    FB2Job getLastJob(FB2Vehicle v) {
        int maxEndTime = 0;
        FB2Job lastJob = null;
        for (FB2Job j : this.getJobs()){
            if (j.vehicle.vehicleID.equals(v.vehicleID)){
                if (maxEndTime < j.jobTiming.endTime){
                    maxEndTime = j.jobTiming.endTime;
                    lastJob = j;
                }
            }
        }
        return lastJob;
    }

    @Override
    public void calcTwistImpact() throws Exception {
        FB2TwistImpactCalculator.calc(cx, this);
    }

    List<FB2Job> getDivJobs(FB2HarvestDivision d) {
        List<FB2Job> divJobs = new ArrayList<FB2Job>();
        for (FB2Job j : this.getJobs()){
            if (j.demand.division.divID.equals(d.divID)){
                divJobs.add(j);
            }
        }
        // sort by ready time
        Collections.sort(divJobs, new Comparator() {

            public int compare(Object o1, Object o2) {

                Integer x1 = ((FB2Job) o1).demand.dueTime;
                Integer x2 = ((FB2Job) o2).demand.dueTime;
                
                // sort low to high?
                int sComp = x1.compareTo(x2);
                return sComp;
                
        }});
        return divJobs;
    }
}
