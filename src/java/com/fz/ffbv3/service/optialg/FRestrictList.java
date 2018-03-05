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
public class FRestrictList {

    ArrayList<FTwist> twists = new ArrayList<FTwist>();
    FSolver solver = null;
    
    public FRestrictList(FSolver solver){
        this.solver = solver;
    }
    
    public void pushFIFO(FTwist twist) {
        twists.add(twist);
        if (twists.size() > solver.getRestrictListMaxSize() ){
            twists.remove(0);
        }
    }
    
    public boolean contains(FTwist t){
        for (FTwist ti : twists){
            if (ti.getUniqID().equals(t.getUniqID()))
                return true;
        }
        return false;
    }

    public int size() {
        return twists.size();
    }
}
