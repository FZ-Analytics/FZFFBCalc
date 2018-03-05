/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.generic.Db;
import com.fz.util.FZUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Eri Fizal
 */
public class FB2SolLogger {
    
    // log to db, and file
    public static void logSol(String title, FB2Solution sol, FB2Context cx) 
            throws Exception {
        // if done mark done
        if ((title.toLowerCase().contains("optimized"))) {
            cx.solution = sol;
            
            logIt(cx, "\n\n\n-------------------\nStart writing solution");
        
            insertJobs(cx, sol);
            
            logDone(cx, "Done");
        }
        
    }

    public static void logToFile(FB2Context cx, String msg) 
            throws Exception {
        FZUtil.logSlowly(cx.runInput.workFolder + "\\log"
                , cx.appName + "_" + cx.runID
                , "\n" + msg);
    }
 
    public static void logDone(FB2Context cx, String msg) 
            throws Exception {
        
        //insert1stJobs(cx);

        // log to db
        // check if runID exist
        String sql = "select 1 from fbSchedRun where runID='" 
                + cx.runID + "'";
        String s1 = FZUtil.queryToItem(cx.con, sql, "");
        logIt(cx, "Check runID exist: " + sql);
        if (s1.equals("1")) {
            sql = 
               "update fbSchedRun set "
                    + " status = 'DONE'"
                    + ", hvsDt = '" + cx.runInput.hvsDate + "'"
                    + ", divIDs = '" + cx.runInput.divIDListStr + "'"
                    + ", msg = '" + FZUtil.escapeText(msg) + "'"
                    + ", lastUpd = " + cx.SQLCurTimeStampFunctinName
                    + ", execStatus = 'INPG'"
                    + " where runID='" + cx.runID + "'"
                    ;
            logIt(cx, "Exist. Update status: " + sql);
        }
        else{
            sql = 
                    "insert into fbSchedRun ("
                    + "runID" 
                    + ", status" 
                    + ", msg" 
                    + ", lastUpd" 
                    + ", execStatus" 
                    + ") values"

                    + "( '" + cx.runID + "'"
                    + ", 'DONE'" 
                    + ", '" + FZUtil.escapeText(msg) + "'" 
                    + ", " + cx.SQLCurTimeStampFunctinName
                    + ", 'INPG'"
                    + ")";
            logIt(cx, "Not Exist. Insert status: " + sql);
        }
        
        FZUtil.queryExecute(cx.con, sql);
        System.out.println(sql);
    }

    public static void logIter(FB2Solver s, FB2Twist t, FB2Context cx) 
            throws Exception {
        
        String msg = 
                "\nIter " + s.getCurrentIteration() 
                + " of " + s.maxIteration
                + ", cycle " + s.curTwistIndex 
                + " of " + s.curMaxTwists 
                //+ ", rstr " + String.valueOf(s.restrictList.size()) 
                //+ ", " + t.getUniqID()
                ;
        
        System.out.println(msg);
        
        // once in a while update db
        if (s.curTwistIndex % 312 == 0){

            String sql = 
               "update fbSchedRun set "
                    + " status = 'INPG'"
                    + ", hvsDt = '" + cx.runInput.hvsDate + "'"
                    + ", divIDs = '" + cx.runInput.divIDListStr + "'"
                    + ", iter = " + String.valueOf(s.getCurrentIteration() + 1) 
                    + ", maxIter = " + String.valueOf(s.maxIteration)
                    + ", subIter = " + String.valueOf(s.curTwistIndex + 1)
                    + ", maxSubIter = " + String.valueOf(s.curMaxTwists)
                    + ", msg = '" + msg + "'"
                    + ", pct = 0" 
                    + ", lastUpd = " + cx.SQLCurTimeStampFunctinName
                    + " where runID='" + cx.runID + "'"
                    ;
            
            try(PreparedStatement ps = cx.con.prepareStatement(sql)){
                ps.executeUpdate();
            }
            
        }
    }

    private static void updateHvsEstmRunID(FB2Context cx, FB2Job j) throws SQLException{
        
        String sql = 
           "update fbHvsEstm set "
                + " runID = '" + cx.runID + "'"
                + " where "
                + " hvsDt = '" + cx.runInput.hvsDate + "'"
                + " and divID = '" + j.demand.division.divID + "'"
                ;

        try(PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }
    
    public static java.sql.Timestamp toLongTime(String hvsDate, int time) 
            throws Exception {
        
        String date1 = hvsDate;
        String time1 = FZUtil.toClock(time);
        String datetime1 = date1 + " " + time1;
        java.sql.Timestamp ts = FZUtil.toSQLTimeStamp(datetime1
                , "yyyy-MM-dd HH:mm");
        return ts;
    }

    private static void insertJobs(FB2Context cx
            , FB2Solution sol) 
            throws SQLException, Exception {
        
        logIt(cx, "Loop insert plan Jobs");
                
        // insert PLAN job for each vehicle
        for (FB2Vehicle v : cx.vehicles){

            // for each jobs
            int vehJobSeq = 0;
            for (FB2Job j : cx.solution.getVehicleJobs(v)){

                // insert PLAN row
                vehJobSeq++;
                insertJobAndTask(j, vehJobSeq, "PLAN", cx, "PLAN");
                
            }
        }
        
        // insert ASGN or NEW job, for 1st job for each div
        
        // create assigned vehicle list
        List<FB2Vehicle> vehAssigned1stJob 
                = new ArrayList<FB2Vehicle>();
        
        // shuffle (randomize order of) divisions
        List<FB2HarvestDivision> shuffledDivs = new ArrayList<>(cx.divisions);
        Collections.shuffle(shuffledDivs); 
        
        // for each div
        for (FB2HarvestDivision d : shuffledDivs){

            // for each div job
            int divJobSeq = 0;
            for (FB2Job j : cx.solution.getDivJobs(d)){
                
                divJobSeq++;

                // if first job in div
                if (divJobSeq == 1){
                    
                    // if the vehicle not assigned any other div 1st job
                    if (!vehAssigned1stJob.contains(j.vehicle)){

                        // assign to cur div
                        vehAssigned1stJob.add(j.vehicle);
                        insertJobAndTask(j, 1, "ASGN", cx, "ACTL");
                    }
                    else{
                        // else, the veh is occupied for other div, create order
                        insertJobAndTask(j, 0, "NEW", cx, "ACTL");
                    }
                }
                else {
                    // not first job, if configure order by algo, then order
                    if (cx.runInput.orderByAlgo
                            .trim().toLowerCase().equals("yes")){
                        
                        insertJobAndTask(j, 0, "NEW", cx, "ACTL");
                    
                    }
                }
            }
        }
    }
    
    private static void insertTasks(FB2Context cx, FB2Job j
            , String doneStatus, String phaseType) 
            throws Exception {
        
        String sql = "INSERT INTO fbTask2"
                + "(JobID"
                + ", From1"
                + ", To1"
                + ", PlanStart"
                + ", PlanEnd"
                + ", DoneStatus"
                + ", FromDesc"
                + ", ToDesc"
                + ", Tonnage"
                + ", Blocks"
                + ", TaskSeq"
                + ", PhaseType"
                + ", createDt"
                + ")"
                + " values (?,?,?,?,?,?,?,?,?,?,?,?,?)"
                ;
        try(PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            // insert 1st task
            int i = 1;
            ps.setInt(i++, j.jobIDInDb); //id
            ps.setString(i++, j.startLoc.name); // fr
            ps.setString(i++, j.demand.loc.name); //to
            ps.setTimestamp(i++
                    , toLongTime(cx.runInput.hvsDate
                            , j.jobTiming.startTime)); //strt
            ps.setTimestamp(i++
                    , toLongTime(cx.runInput.hvsDate
                            , j.jobTiming.timeArriveAtDemand)); // end
            ps.setString(i++, doneStatus); // stat
            ps.setString(i++, j.startLoc.name); // fr dsc
            ps.setString(i++, j.demand.loc.name); //to dsc
            ps.setDouble(i++, j.demand.size); // ton
            ps.setString(i++, j.demand.binBlocks); // blk
            ps.setInt(i++, 1); // seq
            ps.setString(i++, phaseType); // phs type
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp()); // crt dt
            ps.executeUpdate();
            
            // insert nd task
            i = 1;
            ps.setInt(i++, j.jobIDInDb); //id
            ps.setString(i++, j.demand.loc.name); //fr
            ps.setString(i++, j.endLoc.name); // to
            ps.setTimestamp(i++
                    , toLongTime(cx.runInput.hvsDate
                            , j.jobTiming.timeDepartToMill)); //strt
            ps.setTimestamp(i++
                    , toLongTime(cx.runInput.hvsDate
                            , j.jobTiming.endTime)); // end
            ps.setString(i++, doneStatus); // stat
            ps.setString(i++, j.demand.loc.name); //fr
            ps.setString(i++, j.endLoc.name); // to
            ps.setDouble(i++, j.demand.size); // ton
            ps.setString(i++, j.demand.binBlocks); // blk
            ps.setInt(i++, 2); // seq
            ps.setString(i++, phaseType); // phs type
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp()); // crt dt
            ps.executeUpdate();
        }

    }

    private static void insertJobAndTask(FB2Job j, int jobSeq
        , String doneStatus, FB2Context cx
        ,String phaseType) 
        throws Exception {

        String sql = "INSERT INTO fbjob"
            + "(PlanTruckID"
                + ", ActualTruckID"
                + ", JobSeq"
                + ", DoneStatus"
                + ", divID"
                + ", assignedDt"
                + ", createDt"
                + ", runID"
                + ", hvsDt"
                + ", updDt"
                + ", betweenBlock1"
                + ", betweenBlock2"
                + ", readyTime"
                + ", estmKg"
                + ", createSource"
                + ", requesterID"
                + ", remark"
                + ")"
                + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                ;
        try(PreparedStatement ps = cx.con.prepareStatement(sql
            , Statement.RETURN_GENERATED_KEYS)){

            // parse block
            String block1 = "";
            String block2 = "";
            String delim = ";";
            if (j.demand.binBlocks.contains(",")) delim = ",";
            String[] block = j.demand.binBlocks.split(delim);
            if (block.length >= 1 ) block1 = block[0].trim();
            if (block1.length() > 5) {
                throw new Exception("Invalid block1 " + block1);
            }
            if (block.length >= 2 ) block2 = block[1].trim();
            if (block2.length() > 5) {
                throw new Exception("Invalid block2 " + block2);
            }

            String vehicleID = "0";
            int jobSeq2 = 0; 
            
            if (!doneStatus.equals("NEW")){
                
                vehicleID = j.vehicle.vehicleID;
                jobSeq2 = jobSeq;
            }
            
            // insert job
            int i = 1;
            ps.setString(i++, vehicleID);
            ps.setString(i++, vehicleID);
            ps.setInt(i++, jobSeq2);
            ps.setString(i++, doneStatus);
            ps.setString(i++, j.demand.division.divID);
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp());
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp());
            ps.setString(i++, cx.runID);
            ps.setTimestamp(i++
                    , FZUtil.toSQLTimeStamp(
                            cx.runInput.hvsDate, "yyyy-MM-dd")); 
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp());
            ps.setString(i++, block1);
            ps.setString(i++, block2);
            ps.setString(i++, FZUtil.toClock(j.demand.dueTime));
            ps.setDouble(i++, j.demand.size);
            ps.setString(i++, "ALGO");
            ps.setInt(i++, j.demand.division.prodAstUserID); 
            ps.setString(i++, j.demand.division.remark); 

            ps.executeUpdate();

            if (!doneStatus.equals("NEW")){
                
                // insert tasks
                try (ResultSet rs = ps.getGeneratedKeys()){
                    if (rs.next()){

                        j.jobIDInDb = rs.getInt(1);
                        insertTasks(cx, j, doneStatus, phaseType);
                        updateHvsEstmRunID(cx, j);
                        updateVehicleRunID(cx, j, doneStatus);

                    }
                }
            }
        }

    }

    private static void updateVehicleRunID(FB2Context cx, FB2Job j
            , String doneStatus) 
            throws Exception {
        
        String statusSql = "";
        if (doneStatus.equals("ASGN")){
            statusSql = ", status = 'ASGN'";
        }
        
        String sql = 
           "update fbVehicle set "
                + " lastRunID = '" + cx.runID + "'"
                + ", canGoHome = '0'"
                + statusSql
                + " where "
                + " vehicleID = '" + j.vehicle.vehicleID + "'"
                ;

        try(PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }

//    private static void orderUnAssignedDiv1stJob(FB2Context cx
//            , FB2Solution sol
//            , List<FB2HarvestDivision> assignedDivs
//    ) throws Exception {
//        
//        for (FB2HarvestDivision div : cx.divisions){
//    
//            // if assigned, skip
//            if (assignedDivs.contains(div)) continue;
//            
//            // the div not assigned
//            // order the 1st job
//            
//            // find div 1st job
//            FB2Job div1stJob = null;
//            for (FB2Job j : sol.getJobs()){
//                if (j.demand.division.equals(div)){
//                    if (div1stJob == null){
//                        div1stJob = j;
//                    }
//                    else if (j.jobTiming.startTime
//                            < div1stJob.jobTiming.startTime){
//                        div1stJob = j;
//                    }
//                }
//            }
//            if (div1stJob != null){
//                
//                insertJobAndTask(div1stJob, 1, "NEW", cx, "ACTL");
//                
//            }
//        }
//    }

    
    
    public static void logIt(FB2Context cx, String m) throws Exception {

        String drive = "C";
        String logFolder = "fza\\log";
        try {
            FZUtil.logSlowly("D:\\" + logFolder, cx.appName, m);
        }
        catch (Exception e){
            FZUtil.logSlowly("C:\\" + logFolder, cx.appName, m);
        }
    }
}
