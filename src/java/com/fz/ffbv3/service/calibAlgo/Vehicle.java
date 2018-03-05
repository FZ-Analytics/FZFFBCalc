/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.calibAlgo;

import com.fz.ffbv3.service.ffbcalc.FB2Location;

/**
 *
 */
public class Vehicle {
    public String vehicleID;
    public FB2Location loc;
    public String status = "AVLB";
    public String lastRunID;
    public String lastJobStatus;
    public int jobCount;
    public String lastJobID;
    public int canGoHome;
    public String lastTaskActualEnd;
}
