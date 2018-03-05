/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FRestrictList;
import com.fz.ffbv3.service.optialg.FSolution;
import com.fz.ffbv3.service.optialg.FTwist;
import com.fz.util.FZUtil;

/**
 *
 * @author Eri Fizal
 */
public class FB2TwistSwap 
    extends FB2Twist {
    
    private String job2ID = "";
    
    public FB2TwistSwap(
            String job1ID
            , String job2ID
            , FB2Context cx
            , FB2Solution sol 
    ) throws Exception {
        super(job1ID, cx, sol);
        
        this.job2ID = job2ID;
        this.id = createSwapUniqID();
    }
    
    @Override
    public FSolution doTwist(FRestrictList restrictList) throws Exception {

        FB2Solution sol2 = (FB2Solution) sol;
        FB2Job job1 = sol2.getJob(job1ID);
        FB2Job job2 = sol2.getJob(job2ID);
        
        // swap job
        FB2Vehicle v = job1.vehicle;
        job1.vehicle = job2.vehicle;
        job2.vehicle = v;
        
//        // recalc
//        FB2TwistImpactCalculator.calc(this.cx, sol2);
//        
        return sol2;
    }

    public String createSwapUniqID(){
        // ensure order of job1 & 2 doesnt matter
        String s1 = job1ID;
        String s2 = job2ID;
        int i = s1.compareTo(s2);
        String s = "";
        if (i<0){
            s = s1 + " X " + s2;
        }
        else {
            s = s2 + " X " + s1;
        }
        return "Swap: " + s;
    }
    
}
