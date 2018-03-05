/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.optialg;

/**
 *
 * @author Eri Fizal
 */
public abstract class FTwist {
    protected FSolution sol = null;

    public FTwist(FSolution clonedSol) throws Exception {
        this.sol = clonedSol;
        this.sol.setTwist(this);
    }
    
    public abstract FSolution doTwist(
        FRestrictList restrictList
        ) throws Exception ;
    
    public abstract String getUniqID();

    public FSolution getSolution() {
        return sol;
    }
}
