/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.ffbv3.service.optialg.FRestrictList;
import com.fz.ffbv3.service.optialg.FSolution;
import com.fz.ffbv3.service.optialg.FSolver;
import com.fz.ffbv3.service.optialg.FTwist;
import com.fz.util.FZUtil;
import java.util.ArrayList;

/**
 *
 * @author Eri Fizal
 */
public class FB2Solver extends FSolver {

    FB2Context cx = null;
    
    FB2Solver(FB2Context cx) {
        this.cx = cx;
    }

    @Override
    protected FSolution createInitialSolution(FSolution problem) 
            throws Exception {
        FB2Solution sol = (new FB2InitialSolutionBuilder()).build(cx);
        FB2SolLogger.logSol("initial", sol, cx);
        this.maxIteration = cx.runInput.maxIteration;
        return sol;
    }

    @Override
    protected boolean isTimeToTerminate() {
        return (currentIteration++ >= maxIteration);
    }

    @Override
    protected ArrayList<FTwist> createTwistCandidates(
            FSolution sol
    ) throws Exception {
        
       return (new FB2TwistCandidatesMaker()).make(
               cx
               , (FB2Solution) sol
               , restrictList
               , this
       );
    }

    @Override
    protected boolean isBetterQualityThan(String when
            , FSolution solA, FSolution solB) 
            throws Exception {
        return FB2QualityComparator.isBetterQualityThan(when, solA, solB, cx);
   }
    
    @Override
    protected boolean beforeTwist(FTwist twist) 
            throws Exception
    {
        FB2SolLogger.logIter(this, (FB2Twist) twist, cx);
        return true;
    }

    protected boolean beforeIteratingTwists(ArrayList<FTwist> twistCands)
            throws Exception {
        
        // check if user pressed done
        String sql = "select status from fbSchedRun where runID='" 
                + cx.runID + "'";
        String status = FZUtil.queryToItem(cx.con, sql, "");
        if (status.equals("DONE")){
            return false;
        }
        return true;
    }
}
