/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.optialg;

import java.util.ArrayList;

/**
 *
 * @author Eri Fizal
 */
public abstract class FSolution {

    private FTwist twist = null;
    
    public abstract FSolution cloneIt() throws Exception;

    public FTwist getTwist(){
        return twist;
    }

    public void setTwist(FTwist twist) throws Exception {
        this.twist = twist;
    }

    protected void cleanUp() {
    }

    public abstract void calcTwistImpact() throws Exception ;

}
