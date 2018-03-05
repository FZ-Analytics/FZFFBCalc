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
class FB2SingleDivTaskSolver {

    static FB2Solution solve(FB2Context cx) throws Exception {
        
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
                
                // create job
                FB2Job j = new FB2Job(cx, v, d, startTime, loc);
                j.calcTimeAndLoc(startTime, loc);
                
                // add to cur truck
                initSol.addJob(j);

                // update vars for next iter
                startTime = j.jobTiming.endTime + 1;
                loc = j.endLoc;
                
            }
            
        }
        return initSol;
    }
    
}
