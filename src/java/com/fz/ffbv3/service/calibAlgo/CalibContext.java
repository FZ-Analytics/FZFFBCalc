/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.calibAlgo;

import com.fz.ffbv3.service.ffbcalc.FB2RunInput;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CalibContext {
    
    public List<Order> orders = new ArrayList<Order>();
    public List<Vehicle> vehicles = new ArrayList<Vehicle>();
    //public FB2RunInput runInput;
    public Connection con;
    //public String runID;
    //public MsgLogger msgLogger = new MsgLogger();
    public StringBuffer fullLog = new StringBuffer();
    
    public String drive = "C";
    public String logFolder = "\\fza\\log\\";
    public String appName = "fzffbcacl";
    
//    public void log(String m) throws Exception {
//        msgLogger.log(m);
//    }
    
}
