/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FRestrictList;
import com.fz.ffbv3.service.optialg.FSolver;
import com.fz.ffbv3.service.optialg.FTwist;
import java.util.ArrayList;

/**
 *
 * @author Eri Fizal
 */
public class FB2TwistCandidatesMaker {
    public ArrayList<FTwist> make(
            FB2Context cx
            , FB2Solution sol
            , FRestrictList restrictList
            , FSolver solver
    ) throws Exception {
        
        ArrayList<FTwist> twists = new ArrayList<FTwist>();
        
        // for each jobs 1
        for (FB2Job j1 : sol.getJobs()){
            
            // assign to diff vehicle
            // for each vehicle
            for (FB2Vehicle v : cx.vehicles){
                
                // if same vehicle, skip
                if (v.vehicleID.equals(j1.vehicle.vehicleID)) continue;
                
                // create twist
                FB2TwistChangeVehicle t = new FB2TwistChangeVehicle(
                        j1.getID(), v, cx, (FB2Solution) sol.cloneIt());
                twists.add(t);
            }
            
            // swap
            for (FB2Job j2 : sol.getJobs()){
                
                // if same vehicle, skip
                if (j1.vehicle.vehicleID.equals(j2.vehicle.vehicleID)) continue;
                
                // swap j1 and j2
                FB2TwistSwap t1 = new FB2TwistSwap(
                        j1.getID()
                        , j2.getID()
                        , cx
                        , (FB2Solution) sol.cloneIt());
                twists.add(t1);
                
            }
        }
//        FB2SolLogger.logDebug(
//                "\n--------- Iter = " 
//                + solver.getCurrentIteration()
//                + ", Twist Candidates = " 
//                + String.valueOf(twists.size())
//                , cx
//        );
        return twists;
    }
//    public ArrayList<FTwist> make2(
//            FB2Context cx
//            , FB2Solution sol
//            , FRestrictList restrictList
//            , FSolver solver
//    ) throws Exception {
//        
//        ArrayList<FTwist> twists = new ArrayList<FTwist>();
//        
//        // for each jobs 1
//        for (FB2Job j1 : sol.getJobs()){
//            
//            // for each jobs 2
//            for (FB2Job j2 : sol.getJobs()){
//                
//                // if different job
//                if (j1.getID().equals(j2.getID())) continue;
//
//                // create twists
//                
//                // if different vehicle
//                if (!j1.vehicle.vehicleID.equals(j2.vehicle.vehicleID)) {
//                    
//                    // swap j1 and j2
//                    FB2Twist t1 = new FB2TwistSwap(j1, j2, cx);
//                    twists.add(t1);
//
//                    // add j1 remove j2
//                    FB2Twist t2 = new FB2TwistTakeOver(
//                            j1, j2, cx);
//                    twists.add(t2);
//
//                    // add j2 remove j1
//                    FB2Twist t3 = new FB2TwistTakeOver(
//                            j2, j1, cx);
//                    twists.add(t3);
//
//                }
//                
//            }
//            
//            // wake up unassigned vehicle
//
//            // for each vehicle
//            for (FB2Vehicle v: cx.vehicles){
//                
//                // if no job
//                if (sol.getVehicleJobs(v).size() == 0){
//                    
//                    // set j1 to v
//                    FB2Twist t4 = new FB2TwistWakeUp(j1, v, cx);
//                    twists.add(t4);
//                }
//            }
//        }
//        FB2SolLogger.logDebug(
//                "\n--------- Iter = " 
//                + solver.getCurrentIteration()
//                + ", Twist Candidates = " 
//                + String.valueOf(twists.size())
//                , cx
//        );
//        return twists;
//    }
}
