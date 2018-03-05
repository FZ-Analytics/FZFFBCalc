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
public class FB2Demand {
    
    public FB2HarvestDivision division = null;
    public FB2Location centeroid = null;
    public int dueTime = 0;
    public double size = 0;
    public int demandSeq = 0;
    public String demandID = "";
    public int status = FB2Context.DS_UNPICKED;
    public String binBlocks = "";
    public FB2Location loc = null;

    public String getDemandID() {
        return division.divID + "_" + demandSeq;
    }
    
}
