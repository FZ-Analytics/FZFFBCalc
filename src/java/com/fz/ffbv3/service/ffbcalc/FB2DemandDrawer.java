/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

/**
 *
 * @author Eri Fizal
 */
public class FB2DemandDrawer {
    public static String draw(FB2Context cx) throws Exception {
        // for each block
        String code = "";
        for (FB2HarvestDivision d : cx.divisions){
            for (FB2Block b : d.blocks){
                code  += "\nab(" 
                        + b.x1 
                        + "," + b.y1 
                        + ",'" + b.blockID + "'" 
                        + ",'" + d.divID + "'" 
                        + "," + String.valueOf(b.demandSize)
                        + ");"
                        ;
            }
        }
        return code;
    }
    
}
