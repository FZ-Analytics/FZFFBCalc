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
public abstract class FB2Twist extends FTwist {

    public String job1ID;
    protected FB2Context cx = null;
    protected String id = "";
    
    public FB2Twist(
            String job1ID
            , FB2Context cx
            , FB2Solution sol
    ) throws Exception {
        super(sol);
        this.job1ID = job1ID;
        this.cx = cx;
    }

    @Override
    public String getUniqID() {
        return id; 
    }

}
