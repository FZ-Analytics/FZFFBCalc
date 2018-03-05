/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FRestrictList;
import com.fz.ffbv3.service.optialg.FSolution;
import com.fz.ffbv3.service.optialg.FTwist;

/**
 *
 * @author Eri Fizal
 */
class FB2TwistChangeVehicle extends FB2Twist{

    private final FB2Vehicle vehicle;

    public FB2TwistChangeVehicle(
            String job1ID
            , FB2Vehicle vehicle
            , FB2Context cx
            , FB2Solution sol
    ) throws Exception {
        super(job1ID, cx, sol);
        this.vehicle = vehicle;
        this.id = "ChgVhc: " + this.job1ID + " -> " + vehicle.vehicleID;
    }
    
    @Override
    public FSolution doTwist(FRestrictList restrictList) throws Exception {
        
        FB2Solution sol2 = (FB2Solution) sol;
        FB2Job job1 = sol2.getJob(job1ID);
        
        // clone job 1
        FB2Job j1clone = job1.cloneIt();
        
        // set job 1 clone vehicle to be job1 vehicle
        j1clone.vehicle = vehicle;
        
        // add job 2b 
        sol2.addJob(j1clone);
        
        // remove job 2
        FB2Job removedJob = sol2.removeJob(job1ID);
        if (removedJob == null) throw new Exception("Unable to remove job: "
                + job1ID);
        
//        // recalc
//        FB2TwistImpactCalculator.calc(this.cx, sol2);
//        
        return sol2;
    }
    
}
