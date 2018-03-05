/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import java.sql.Connection;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

/**
 *
 * @author Eri Fizal
 */
public class FB2Context {
    
    // constants
    public String appName = "ffbv3Algo";
    
    // input
    public FB2RunInput runInput = null;
    public Connection con;
    
    // old input
    public String SQLCurTimeStampFunctinName = "current_timestamp()";

    // demand status
    public static final int DS_UNPICKED = 0;
    public static final int DS_PICKED = 1;
    
    // event status
    public static final int EV_DEPART = 0;
    public static final int EV_ARRIVE = 1;
        
    // run params
    public String runID;
    public int curTime = 0;
    public double totalDemandSize = 0;
    
    public FB2Solution problem = null;
    public FB2Solution solution = null;
    
    public ArrayList<FB2HarvestDivision> divisions 
            = new ArrayList<FB2HarvestDivision>();
    public ArrayList<FB2Demand> demands = new ArrayList<FB2Demand>();
    public ArrayList<FB2Vehicle> vehicles = new ArrayList<FB2Vehicle>();
    public ArrayList<FB2Trip> trips 
            = new ArrayList<FB2Trip>();

}
