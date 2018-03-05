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
public class FB2InitialSolutionBuilder {
    
    public FB2Solution build(FB2Context cx) throws Exception {
        
        FB2Solution initSol = new FB2Solution(cx);
        
        int startTime = -1;
        FB2Location loc = null;
        
        // for each div
        for (FB2HarvestDivision dv : cx.divisions){
            
            // get avail truck
            FB2Vehicle v = initSol.get1UnassignedVehicle();
            
            // if no more truck
            if (v == null){
                // exit
                break;
            }
            startTime = v.earliestDepart;
            loc = v.startLoc;
            
            // for each demand in div
            for (FB2Demand d : cx.demands){
                
                // if not in div continue loop
                if (!d.division.divID.equals(dv.divID)) continue;
                
                // if job feasible
                // create job
                FB2Job j = new FB2Job(cx, v, d, startTime, loc);
                j.calcTimeAndLoc(startTime, loc);
                
                if (j.isFeasible()){
                    
                    // add to cur truck
                    initSol.addJob(j);
                    
                    // update vars for next iter
                    startTime = j.jobTiming.endTime + 1;
                    loc = j.endLoc;

                }
                // else
                else {
                    // leave it for now
                }
                
            }
            
        }
        // round robin remaining demand
        // get vehCount
        int vehCount = cx.vehicles.size();
        
        // vehIndex = -1
        int vehIndex = -1;
        
        // round robin remaining
        // for each demand
        for (FB2Demand d : cx.demands){
            
            // if assigned, skip
            if (initSol.isDemandAssigned(d)) continue;
            
            // vehIndex++
            vehIndex++;
            
            // if vehIndex == vehCount
            if (vehIndex == vehCount){
                // restart vehIndex 
                vehIndex = 0;
            }
            // create job
            // get vehicle
            FB2Vehicle v = cx.vehicles.get(vehIndex);

            // get its last job
            FB2Job lastJob = initSol.getLastJob(v);

            // update time and loc for new job
            if (lastJob == null){
                startTime = v.earliestDepart;
                loc = v.startLoc;
            }
            else {
                startTime = lastJob.jobTiming.endTime + 1;
                loc = lastJob.endLoc;
            }

            // create new job
            FB2Job j = new FB2Job(cx, v, d, startTime, loc);
            j.calcTimeAndLoc(startTime, loc);

            // add
            initSol.addJob(j);
        }
        return initSol;
    }
    
//    public FB2Solution build2(FB2Context cx) throws Exception {
//        
//        FB2Solution initSol = new FB2Solution(cx);
//        
//        // get veh
//        FB2Vehicle v = initSol.get1UnassignedVehicle();
//        if (v == null) throw new Exception("No vehicle");
//        
//        // init vars
//        int startTime = v.earliestDepart;
//        FB2Location loc = v.startLoc;
//        
//        // for each demand
//        for (FB2Demand d : cx.demands){
//            
//            // add job
//            FB2Job j = new FB2Job(cx, v, d, startTime, loc);
//            initSol.addJob(j);
//            
//            // update vars
//            startTime = j.endTime + 1;
//            loc = j.endLoc;
//        }
//        
//        return initSol;
//    }
//    
//    public FB2Solution build(FB2Context cx) throws Exception {
//        
//        FB2Solution initSol = new FB2Solution(cx);
//        
//        // calc demand per vehicle
//        int demandCount = cx.demands.size();
//        int demandPerVeh = (int) Math.ceil(demandCount / cx.vehicles.size());
//        int demandIndex = 0;
//        
//        // for each veh
//        for (FB2Vehicle v : cx.vehicles){
//
//            // init start time & loc
//            int startTime = v.earliestDepart;
//            FB2Location loc = v.startLoc;
//            
//            // for 1 to demandPerVeh
//            for (int i=1; i<=demandPerVeh; i++){
//                
//                // if ++demandIndex < demandCount
//                if (demandIndex < demandCount){
//                    
//                    // get the demand
//                    FB2Demand d = cx.demands.get(demandIndex++);
//                    
//                    // add job
//                    FB2Job j = new FB2Job(cx, v, d, startTime, loc);
//                    initSol.addJob(j);
//                    
//                    // update cur time & loc
//                    startTime = j.endTime + 1;
//                    loc = j.endLoc;
//                }
//                else break;
//            }
//        }
//        return initSol;    
//    }
    
    
}
