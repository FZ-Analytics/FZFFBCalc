/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.optialg;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Eri Fizal
 */
public abstract class FSolver {
    
    public int maxIteration = 100;
    protected int currentIteration = 0;
    public int curMaxTwists = 0;
    public int curTwistIndex = 0;
    private int restrictListMaxSize = 10;
    public FRestrictList restrictList = null;
    
    public int getCurrentIteration() {
        return currentIteration;
    }

    public FSolution solve(FSolution problem) throws Exception {

        FSolution iterSol = null;
        FSolution bestSol = null;
        
        // create initial solution
        iterSol = createInitialSolution(problem);
        bestSol = iterSol;
        restrictList = this.createRestrictList();
        
        // start iterating
        for (currentIteration = 0
                ; currentIteration < maxIteration
                ; currentIteration++){

            // create twist candidates
            ArrayList<FTwist> twistCands = 
                    this.createTwistCandidates(iterSol);
            this.curMaxTwists = twistCands.size();
            this.curTwistIndex = 0;

            // get admissible solution
            if (!beforeIteratingTwists(twistCands)) break;
            iterSol = chooseAdmissibleSolution(
                    iterSol
                    , twistCands
                    , bestSol
            );
            
            // if global iterated solution better than global best
            if (isBetterQualityThan("best", iterSol, bestSol)){

                // update global best 
                bestSol = iterSol;
                afterNewBest(bestSol);
            }

            // update restrict list
            restrictList.pushFIFO(
                    iterSol.getTwist()
                    );

            // try to free memory
            twistCands.clear();
            twistCands = null;
            System.gc();
            //Thread.sleep(100);

        }
        return bestSol;
    }

    private FSolution chooseAdmissibleSolution(
            FSolution iterSol
            , ArrayList<FTwist> twistCands
            , FSolution bestSol
            ) throws Exception {
        
        FSolution bestAdmissibleSol = iterSol;
    
        // for each twist candidates
        for (FTwist twist : twistCands){
            
            // do before twist 
            if (!beforeTwist(twist)) break;
            
            // do twist 
            twist.doTwist(restrictList);
            this.curTwistIndex++;
            
            // do after twist 
            twist.sol.calcTwistImpact();
            if (!afterTwist(twist)) break;
            
            // if better than current best admissible
            boolean admissible = false;
            if (isBetterQualityThan("twist"
                    , twist.getSolution(), bestAdmissibleSol)){
                
                // if restricted
                if (restrictList.contains(twist)){
                    
                    // if met aspiration 
                    if (isBetterQualityThan("aspiration"
                            , twist.getSolution(), bestSol)){
                        
                        // admissible
                        admissible = true;
                    }
                    else { // else not meet aspiration, remain not admissible
                    }
                }
                else {
                    // not restricted, admissible
                    admissible = true;
                }
            }
            // if admissible, record it
            if (admissible){
                bestAdmissibleSol = twist.getSolution();
            }
            else {
                twist.getSolution().cleanUp();
            }
            
            // free memory
            twist = null;
        }
        return bestAdmissibleSol;
    }

    protected FRestrictList createRestrictList() throws Exception {
        return new FRestrictList(this);
    }

    public void setRestrictListMaxSize(int n){
        restrictListMaxSize = n;
    }
    
    public int getRestrictListMaxSize(){
        return restrictListMaxSize;
    }

    //------------ events 
    protected boolean beforeTwist(FTwist twist) 
            throws Exception
    {
        return true;
    }

    protected boolean afterTwist(FTwist twist) 
            throws Exception {
        return true;
    }

    protected boolean afterNewBest(FSolution bestSol) throws Exception {
        return true;
    }
    
    protected boolean beforeIteratingTwists(ArrayList<FTwist> twistCands)
            throws Exception {
        return true;
    }

    //------------ abstract members 
    
    protected abstract FSolution createInitialSolution(FSolution problem)
            throws Exception ;

    protected abstract ArrayList<FTwist> createTwistCandidates(
            FSolution sol) throws Exception ;
    
    protected abstract boolean isTimeToTerminate() throws Exception ;
    
    protected abstract boolean 
        isBetterQualityThan(String when, FSolution sol1, FSolution sol2) 
                throws Exception;

}