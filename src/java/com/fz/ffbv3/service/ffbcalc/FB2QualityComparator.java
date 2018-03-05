/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FSolution;

/**
 *
 * @author Eri Fizal
 */
public class FB2QualityComparator {
    public static boolean isBetterQualityThan(String when
            , FSolution solA, FSolution solB, FB2Context cx) 
            throws Exception {
        
        FB2Solution sol1 = (FB2Solution) solA;
        FB2Solution sol2 = (FB2Solution) solB;
        
        boolean better = false;
        
        // compare restan
        double unhandledSize1 = sol1.calcUnhandledSize();
        double unhandledSize2 = sol2.calcUnhandledSize();
        if ( unhandledSize1 == unhandledSize2){
            
            // compare time
            int maxEndTime1 = sol1.calcMaxEndTime();
            int maxEndTime2 = sol2.calcMaxEndTime();
            if (maxEndTime1 == maxEndTime2){
                
                // compare number of vehicle
                int numOfVehicle1 = sol1.calcNumberOfUsedVehicle();
                int numOfVehicle2 = sol2.calcNumberOfUsedVehicle();
                if (numOfVehicle1 < numOfVehicle2){
                    
                    better = true;
                }
                
            }
            else if (maxEndTime1 < maxEndTime2){
                better = true;
            }
        }
        else if (unhandledSize1 < unhandledSize2){
            
            better = true;
        }
        // log
        if (better) {
            FB2SolLogger.logSol("Better: " + when , sol1, cx);
        }
        else {
            //FB2Logger.logSol("Worse found: " + when , sol1, cx);
        }
        
        return better;
   }
}
