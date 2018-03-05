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
public class FB2Vehicle {
    public String vehicleID = "";
    public String vehicleName = "";
    public int earliestDepart = 0;//cx.vehicleEarliestDepart;
    public String type = "";
    public double capacity = 0; 
    private FB2Context cx;
    FB2Location startLoc;
    String defaultDivID;
    
    FB2Vehicle(FB2Context cx) {
        this.cx = cx;
    }
}
