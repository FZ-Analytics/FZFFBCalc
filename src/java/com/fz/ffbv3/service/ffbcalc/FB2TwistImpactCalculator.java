/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.util.FZUtil;
import java.util.ArrayList;

/**
 *
 * @author Eri Fizal
 */
public class FB2TwistImpactCalculator {

    public static void calc(FB2Context cx, FB2Solution sol) throws Exception {

        // for each vehicle
        for (FB2Vehicle v : cx.vehicles){
            
            // get its jobs
            ArrayList<FB2Job> jobs = sol.getVehicleJobs(v);
            
            // sort by demand due time
            // TODO: what should the sort parameter be? timeBackToMill?
            FB2SorterOfDemandDue.sortByDemandDue(jobs);
            
            // for each job
            int curTime = v.earliestDepart;
            FB2Location curLoc = v.startLoc;
            for (FB2Job j : jobs){
            
                // calc times & loc
                j.calcTimeAndLoc(curTime, curLoc);
                
                // set the time & loc for next
                curTime = j.jobTiming.endTime + 1;
                curLoc = j.endLoc;
            }
        }
    }
}
