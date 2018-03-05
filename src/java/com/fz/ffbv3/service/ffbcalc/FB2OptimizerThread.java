/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.ffbcalc;

import com.fz.generic.Db;
import com.fz.util.FZUtil;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class FB2OptimizerThread extends Thread {

    private String runID;
    
    public FB2OptimizerThread(String runID){
        this.runID = runID;
    }
    
    @Override
    public void run() {
        optimize();
    }

    private void optimize() {
        
        FB2Context cx = new FB2Context();
        cx.runID = runID;
        try {
            
            doOptimize(cx);
            
        } catch (Exception e){
            // TODO
            handleError(e, cx);
        }
    }

    private void doOptimize(FB2Context cx) throws Exception {
        
        try (Connection con = (new Db()).getConnection("jdbc/fz");){
            
            cx.con = con;
            
            // get input json
            String jsonInput = loadJsonFromDb(cx);

            // parse json
            Gson gson = new Gson();
            FB2RunInput runInput = gson.fromJson(jsonInput, FB2RunInput.class);
            formatInput(runInput);
            
            cx.runID = runID;
            cx.runInput = runInput;
            
            // check duplicate run
            checkDuplicateRun(cx);

            // define problem
            FB2Solution problem = createProblemDefinition(cx);

            // solve it
            FB2Solution sol;
            if (cx.runInput.MultiDivPerVehicle
                    .trim().toLowerCase().equals("yes")){
                FB2Solver solver = new FB2Solver(cx);
                sol = (FB2Solution) solver.solve(problem);
            }
            else {
                // single div task
                sol = FB2SingleDivTaskSolver.solve(cx);
            }
            FB2SolLogger.logSol("Optimized Solution", sol, cx);
        }
    }
    
    private FB2Solution createProblemDefinition(FB2Context cx) throws Exception {
        (new FB2DemandBuilder()).buildDemands(cx);
        (new FB2VehicleInitter()).initVehicles(cx);
        FB2Solution problem = new FB2Solution(cx);
        return problem;
    }

    private void handleError(Exception ep, FB2Context cx) {
        
        try {
            
            String s = FZUtil.toStackTraceText(ep);
            String m = FZUtil.escapeText(s);

            // try log to file
            try {
                FB2SolLogger.logToFile(cx, m);
            } catch (Exception e1) 
            {System.out.println(FZUtil.toStackTraceText(e1));}
            
            // try log to db
            try {

                try (Connection con = (new Db()).getConnection("jdbc/fz");){
                    
                    // try select
                    String sql = 
                            "select 1 from fbSchedRun"
                            + " where runID = '" + cx.runID + "'"
                            ;
                    try (PreparedStatement ps = con.prepareStatement(sql);
                            ResultSet rs = ps.executeQuery()){
                        if (rs.next()){
                            sql = 
                                "update fbSchedRun "
                                + " set status = 'ERR'"
                                + ", msg = '" + m + "'"
                                + " where runID = '" + cx.runID + "'"
                                ;
                        }
                        else {
                            sql = 
                                "insert into fbSchedRun"
                                + "(runID, status, msg, hvsDt, divIDs)"
                                + " values(" 
                                + "'" + cx.runID + "'"
                                + ",'ERR'"
                                + ",'" + m + "'"
                                + ",'" + FZUtil.escapeText(cx.runInput.hvsDate) + "'"
                                + ",'" + FZUtil.escapeText(cx.runInput.divIDListStr) + "'"
                                + ")"
                                ;
                        }
                        try (PreparedStatement ps2 = con.prepareStatement(sql)){
                            ps2.executeUpdate();
                        }
                    }

                }

            } catch (Exception e1) {

                Logger.getLogger(FB2OptimizerThread.class.getName()).log(
                        Level.SEVERE, null, e1);
            }
                
        } catch (Exception e1) 
        {System.out.println(FZUtil.toStackTraceText(e1));}
    }
    
    private void formatInput(FB2RunInput runInput) {
        
        runInput.startFruitReadyForGrabber = 
                FZUtil.clockToMin(runInput.startFruitReadyForGrabberStr);
        runInput.millEndTime =
                FZUtil.clockToMin(runInput.millEndTimeStr);
        runInput.divIDListStr = "";
        for (String divID : runInput.divList){
            runInput.divIDListStr += divID + ";";
        }
    }

    private String loadJsonFromDb(FB2Context cx) throws Exception {
        String sql = "select input1 from fbSchedRun where runID = ?";
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.setString(1, cx.runID);
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    String json = rs.getString(1);
                    return json;
                }
                else{
                    throw new Exception("Cannot find progress entry runID " 
                            + cx.runID);
                }
            }
        }
    }

    private void checkDuplicateRun(FB2Context cx) throws Exception {
        
        String criteria = "";
        for (String d : cx.runInput.divList){
            if (criteria.length() > 0) criteria += " or ";
            criteria += "divIDs like '%" + d + ";%'";
        }

        String sql = "select 1 from fbSchedRun"
                + " where hvsDt = '" + cx.runInput.hvsDate + "'"
                + " and status in ('INPG','DONE')"
                + " and (" + criteria + ")"
                ;
        try (PreparedStatement ps = cx.con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
                ){
            
            // if duplicate
            if (rs.next()){
                
                throw new Exception("Duplicate runs."
                        + " Some or all of " + cx.runInput.divIDListStr
                        + " included in other runs"
                        + " for harvest date " + cx.runInput.hvsDate
                );
            }
        }
    }

}
