<%@page import="java.util.Calendar"%>
<%@page import="java.io.FilenameFilter"%>
<%@page import="java.io.File"%>
<%@page import="java.nio.file.Path"%>
<%@page import="java.nio.file.Files"%>
<%@page import="com.fz.util.EObject"%>
<%@page import="com.fz.ffbv3.service.calibAlgo.CalibAlgo"%>
<%@page import="com.fz.ffbv3.service.ffbcalc.FB2SolLogger"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.fz.ffbv3.service.ffbcalc.FB2RunInput"%>
<%@page import="com.google.gson.Gson"%>
<%@page import="com.fz.ffbv3.service.ffbcalc.FB2Location"%>
<%@page import="java.util.List"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="com.fz.util.FZUtil"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="com.fz.generic.Db"%>
<%@page import="java.sql.Connection"%>
<%@page import="com.fz.ffbv3.service.calibAlgo.CalibContext"%>
<%@page import="com.fz.ffbv3.service.calibAlgo.Order"%>
<%@page import="com.fz.ffbv3.service.calibAlgo.Vehicle"%>
<%!
    //String drive = null;
    
    public String run(HttpServletRequest request, HttpServletResponse response
            , PageContext pc) throws Exception {

        CalibContext cx = new CalibContext();
        try {
            
            // get passes parameter
            if (new File("D:/").exists()){
                cx.drive = "D";
            }

            logIt(cx, "\n\n\n------------------------");
            logIt(cx, "Log to " + cx.drive + ":\\fza\\log\\");

            // connect to db
            logIt(cx, "Connecting to db");
            try (Connection con = (new Db()).getConnection("jdbc/fz");){

                // keep connection object
                logIt(cx, "DB Connected");
                cx.con = con;
                
                // check if other instance running
                if (!otherIsRunning(cx)){
                    
                    // add instance
                    addInstance(cx);
                    
                    // run calc
                    logIt(cx, "Calc run");
                    runCalc(cx);
                    
                    // remove instance
                    removeInstance(cx);
                    logIt(cx, "Algo finish");

                    // clean up log
                    cleanUpLog(cx);
                }
                else {
                    // other is run
                    logIt(cx, "Other already running");
                }

            }

        } catch (Exception e){
            
            // log
            String m = FZUtil.toStackTraceText(e);
            try {
                // attempt to clear instance
                logIt(cx, "ERROR:\n" + m);
                try (Connection con = (new Db()).getConnection("jdbc/fz");){
                    removeInstance(cx);
                }
                
            } catch (Exception e1){
            }
        }
        
        // return logs
        String return1 = cx.fullLog.toString();
        return return1;
    }

    public  boolean otherIsRunning(CalibContext cx) 
        throws Exception {
        
        boolean isRunning = false;
        String sql = "select 1 from fbAIRun";
        logIt(cx, "Check other instance: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()){
            if (rs.next()){
                isRunning = true;
            }
        }
        return isRunning;
    }

    public void removeInstance(CalibContext cx) throws Exception {
        String sql = "delete from fbAIRun";
        logIt(cx, "Remove instance: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }

    public void addInstance(CalibContext cx) throws Exception {
        String sql = "insert into fbAIRun(updDt) values(?)";
        logIt(cx, "Add instance: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.setTimestamp(1, FZUtil.getCurSQLTimeStamp());
            ps.executeUpdate();
        }
    }

    void runCalc(CalibContext cx) throws Exception {
        
        // get runID where today & execStatus = INPG
        List<String> runIDs = getINPGRunIDs(cx);
        
        // for each runID
        logIt(cx, "RunIDs to process = " + runIDs.size());
        for (String runID : runIDs){

            logIt(cx, "Current runID " + runID);
            
            // get order list
            //OrderManager om = new OrderManager();
            loadOrders(cx, runID);
            logIt(cx, "Orders due = " + cx.orders.size());
            if (cx.orders.isEmpty()) {
                continue;
            }
            
            // get vehicle list, with total job, total dur
            //VehicleMgr vm = new VehicleMgr();
            loadVehicles(cx);
            logIt(cx, "Vehicles count = " + cx.vehicles.size());
            if (cx.vehicles.isEmpty()) {
                continue;
            }

            // sort toProcessOrder by dur, small to large
            // sort vehicle by total job, small to large
            // for each toProcessOrder
            for (Order o : cx.orders){

                logIt(cx, "Finding vehicle for order/job " + o.jobID);
                
                // get 1 vehicle
                Vehicle v = get1AvailVehicle(o, cx);

                // if exist
                if (v != null){

                    // assign
                    assign(cx, o, v);
                }
                // else, break, no more vehicle
                else {
                    
                    logIt(cx, "No more avail vehicle ");
                    break;
                } 
            }
            // determine who can go home
            checkWhoCanGoHome(cx, runID);

        }

        // calibrate the configuration to reach optimum level
        CalibAlgo.runArtificialIntelligence(cx);

    }

    public void checkWhoCanGoHome(CalibContext cx, String runID) 
            throws Exception {

        logIt(cx, "checkWhoCanGoHome");

        // get last job per div for this runID
        String sql = "select"
                + " j.divID, j.doneStatus, j.isLastOrder, j.jobID"
                + " from"
                + " ("
                + "	select"
                + "		divID"
                + "		, max(jobID) lastJobID"
                + "	from fbJob "
                + "	where"
                + "		runID = '" + runID + "'"
                + "		and DoneStatus in('NEW','ASGN','TKEN','DONE')"
                + "		and reOrderToJobID is null"
                + "	group by "
                + "		divID"
                + " ) divLastJobs"
                + " left join fbJob j"
                + "	on divLastJobs.lastJobID = j.jobID ";

/*        String sql2 = "select *"
                + " from ("
                    + "select "
                        + "divID"
                        + ", status"
                        + ", isLastJob"
                        + ", jobID"
                        + ", rownum() over(partition by divID order by createDt desc) rowNbr"
                    + " from fbJob"
                    + " where runID='" + runID + "'"
                        + " and DoneStatus in ('NEW','ASGN','TKEN','DONE')"
                        + " and reOrderToJobID is null"
                    + ") divLastJobs"
                + " where rowNbr = 1"
                ;
*/

        // get div last job
        logIt(cx, "Get last job each divs: " + sql);
        List<EObject> lastJobs = new ArrayList<EObject>();
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            // query
            try (ResultSet rs = ps.executeQuery()){
                
                while (rs.next()){

                    EObject lastJob = new EObject();
                    lastJob.putStr("divID", FZUtil.getRsString(rs, 1, ""));
                    lastJob.putStr("doneStatus", FZUtil.getRsString(rs, 2, ""));
                    lastJob.putStr("isLastOrder", FZUtil.getRsString(rs, 3, ""));
                    lastJob.putStr("jobID", FZUtil.getRsString(rs, 4, ""));
                    lastJobs.add(lastJob);

                    logIt(cx, "Got last job of div"
                        + ", jobID: " + lastJob.getStr("jobID")
                        + ", div: " + lastJob.getStr("divID")
                        + ", status: " + lastJob.getStr("doneStatus")
                        + ", isLast: " + lastJob.getStr("isLastOrder")
                    );

                }
            }

            // loop each divLastJob
            boolean allDivIsLastJobAndAssigned = true;
            for (EObject lastJob : lastJobs){

                // if is last job
                if (lastJob.getStr("isLastOrder").equals("yes")){

                    // if assigned
                    if (!lastJob.getStr("doneStatus").equals("NEW")){

                        logIt(cx, "Last order was assigned "
                            + ", jobID: " + lastJob.getStr("jobID")
                            + ", div: " + lastJob.getStr("divID")
                            + ", status: " + lastJob.getStr("doneStatus")
                            + ", isLast: " + lastJob.getStr("isLastOrder")
                        );

                        // continue loop
                        continue;
                    }
                    else {
                        logIt(cx, "Last order NOT assigned yet"
                            + ", jobID: " + lastJob.getStr("jobID")
                            + ", div: " + lastJob.getStr("divID")
                            + ", status: " + lastJob.getStr("doneStatus")
                            + ", isLast: " + lastJob.getStr("isLastOrder")
                        );

                    }
                }
                else {
                    logIt(cx, "NOT last order yet"
                        + ", jobID: " + lastJob.getStr("jobID")
                        + ", div: " + lastJob.getStr("divID")
                        + ", status: " + lastJob.getStr("doneStatus")
                        + ", isLast: " + lastJob.getStr("isLastOrder")
                    );
                }

                // reach here means at least 1 not last job
                // , or last job but unassigned
                allDivIsLastJobAndAssigned = false;
                break;
            }

            // if all is last job and assigned, can go home 
            int canGoHome = (allDivIsLastJobAndAssigned ? 1 : 0);

            logIt(cx, "canGoHome = " + canGoHome);

            // release all vehicle
            String vehicleIDs = "";
            for (Vehicle v : cx.vehicles){
                if (vehicleIDs.length() > 0) vehicleIDs += ",";
                vehicleIDs += v.vehicleID;
            }
            sql = "update fbVehicle set canGoHome = '" + canGoHome + "'"
                + " where vehicleID in (" + vehicleIDs + ")" ;

            logIt(cx, "Release vehicle Sql = " + sql);

            try (PreparedStatement ps2 = cx.con.prepareStatement(sql)){
                ps2.executeUpdate();
            }

        }
    }

    public  void assign(CalibContext cx, Order o, Vehicle v) 
            throws Exception {
        
        v.status = "ASGN";
        
        cx.con.setAutoCommit(false);
   
        // get job seq
        String sql = "select "
                + " max(jobSeq) "
                + " from fbJob"
                + " where runID = '" + o.runID + "'"
                + " and actualTruckID = '" + v.vehicleID + "'"
                + " and doneStatus <> 'PLAN'"
                + " and reorderToJobID is null"
                ;
        logIt(cx, "Get job seq: " + sql);
        int jobSeq = 0;
        try(PreparedStatement ps = cx.con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()){
            rs.next();
            jobSeq = rs.getInt(1);
        }
        jobSeq++;
        
        // sql to update job
        sql = "update fbJob "
                + "set doneStatus = 'ASGN'"
                + ", planTruckID = '" + v.vehicleID + "'"
                + ", actualTruckID = '" + v.vehicleID + "'"
                + ", assignedDt = current_timestamp"
                + ", jobSeq = " + String.valueOf(jobSeq)
                + " where jobID = '" + o.jobID + "'"
                ;
        logIt(cx, "Update job: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
        
        // sql to insert task
        sql = "INSERT INTO fbTask2"
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
        logIt(cx, "Insert tasks: " + sql);
        try(PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            // insert 1st task
            int i = 1;
            ps.setString(i++, o.jobID); //id
            ps.setString(i++, o.millLoc.name); // fr
            ps.setString(i++, o.betweenBlock1 + " " + o.betweenBlock2); //to
            ps.setTimestamp(i++, FB2SolLogger.toLongTime(
                    o.runInput.hvsDate, o.jobTiming.startTime)); //strt
            ps.setTimestamp(i++, FB2SolLogger.toLongTime(
                    o.runInput.hvsDate, o.jobTiming.timeArriveAtDemand)); // end
            ps.setString(i++, "ASGN"); // stat
            ps.setString(i++, o.millLoc.name); // fr
            ps.setString(i++, o.betweenBlock1 + " " + o.betweenBlock2); //to
            ps.setDouble(i++, o.estmKg); // ton
            ps.setString(i++, o.betweenBlock1 + ";" + o.betweenBlock2); // blk
            ps.setInt(i++, 1); // seq
            ps.setString(i++, "ACTL"); // phs type
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp()); // crt dt
            ps.executeUpdate();
            
            // insert nd task
            i = 1;
            ps.setString(i++, o.jobID); //id
            ps.setString(i++, o.betweenBlock1 + " " + o.betweenBlock2); //fr
            ps.setString(i++, o.millLoc.name); // to
            ps.setTimestamp(i++, FB2SolLogger.toLongTime(
                    o.runInput.hvsDate, o.jobTiming.timeDepartToMill)); //strt
            ps.setTimestamp(i++, FB2SolLogger.toLongTime(
                    o.runInput.hvsDate, o.jobTiming.endTime)); // end
            ps.setString(i++, "ASGN"); // stat
            ps.setString(i++, o.betweenBlock1 + " " + o.betweenBlock2); //fr
            ps.setString(i++, o.millLoc.name); // to
            ps.setDouble(i++, o.estmKg); // ton
            ps.setString(i++, o.betweenBlock1 + ";" + o.betweenBlock2); // blk
            ps.setInt(i++, 2); // seq
            ps.setString(i++, "ACTL"); // phs type
            ps.setTimestamp(i++, FZUtil.getCurSQLTimeStamp()); // crt dt
            ps.executeUpdate();
        }
        
        // sql to update vehicle
        sql = "update fbVehicle "
                + "set status = 'ASGN'"
                + " where vehicleID = '" + v.vehicleID + "'"
                ;
        logIt(cx, "Update vehicle: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
        
        logIt(cx, "Commit");
        cx.con.setAutoCommit(true);
    }

     List<String> getINPGRunIDs(CalibContext cx) 
            throws Exception {
        
        List<String> runIDs = new ArrayList<String>();
        
        String sql = "select "
                + " runID "
                + " from fbSchedRun "
                + " where hvsDt = curdate()"
                + " and execStatus = 'INPG' ";
        logIt(cx, "Get runIDs to process: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                
                String runID = rs.getString(1); 
                runIDs.add(runID);
                
                logIt(cx, "Got runID " + runID);
            }
        }
        
        return runIDs;
    }
    void loadOrders(CalibContext cx, String runID) throws Exception {
        
        cx.orders = new ArrayList<Order>();
        
        // get order for this runID
        String sql = "select "
                + " j.jobID"
                + ", j.divID"
                + ", j.betweenBlock1"
                + ", j.betweenBlock2"
                + ", j.readyTime"
                + ", j.estmKg"
                + ", m.lon"
                + ", m.lat"
                + ", m.millID"
                + ", b.x1"
                + ", b.y1"
                + ", p.input1"
                + ", j.isLastOrder"
                + " from fbJob j"
                + "     left outer join fbBlock b"
                + "         on j.betweenBlock1 = b.blockID"
                + "         and j.divID = b.divID"
                + "     left outer join fbDiv d"
                + "         on b.divID = d.divID"
                + "     left outer join fbMill m"
                + "         on m.millID = d.millID"
                + "     left outer join fbSchedRun p"
                + "         on j.runID = p.runID"
                + " where j.runID = '" + runID + "'"
                + "     and j.doneStatus = 'NEW'"
                + "     and j.reorderToJobID is null"
                + " order by j.readyTime"
                ;
        
        logIt(cx, "Get orders: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            // query
            try (ResultSet rs = ps.executeQuery()){
                
                while (rs.next()){
                    
                    Order o = new Order();
                    
                    // get fields
                    o.jobID = FZUtil.getRsString(rs, 1, "");
                    o.divID = FZUtil.getRsString(rs, 2, "");
                    o.betweenBlock1 = FZUtil.getRsString(rs, 3, "");
                    o.betweenBlock2 = FZUtil.getRsString(rs, 4, "");
                    o.readyTimeStr = FZUtil.getRsString(rs, 5, "");
                    o.readyTime = FZUtil.clockToMin(o.readyTimeStr);
                    o.estmKg = FZUtil.getRsDoubleErr(rs, 6
                            , "Invalid Estm Kg for job " + o.jobID);
                    
                    // get mill loc
                    double millLon = FZUtil.getRsDoubleErr(rs, 7
                            , "Invalid mill lon for job " + o.jobID);
                    double millLat = FZUtil.getRsDoubleErr(rs, 8
                            , "Invalid mill lat for job " + o.jobID);
                    FB2Location millLoc = new FB2Location();
                    millLoc.lon = millLon;
                    millLoc.lat = millLat;
                    millLoc.name = FZUtil.getRsString(rs, 9, "");
                    o.millLoc = millLoc;
                    
                    // get block loc
                    double block1Lon = FZUtil.getRsDoubleErr(rs, 10
                            , "Invalid block1 lon for job " + o.jobID);
                    double block1Lat = FZUtil.getRsDoubleErr(rs, 11
                            , "Invalid block1 lat for job " + o.jobID);
                    FB2Location block1Loc = new FB2Location();
                    block1Loc.lon = block1Lon;
                    block1Loc.lat = block1Lat;
                    block1Loc.name = o.betweenBlock1;
                    o.block1Loc = block1Loc;
                    
                    // get run input
                    Gson gson = new Gson();
                    String json = FZUtil.getRsString(rs, 12, "");
                    o.runInput = gson.fromJson(json, FB2RunInput.class);
                    o.runID = runID;
                    
                    logIt(cx, "Got job order: " + o.jobID 
                            + ", runID: " + o.runID
                            + ", between block: " + o.betweenBlock1
                    );
                    
                    // check is due
                    //TripCalculator tc = new TripCalculator(cx, o);
                    if (calcIsDue(o, cx)){
                        
                        // if due, add for procesing
                        cx.orders.add(o);
                    }
                    else {
                        // dont add
                    }
                    
                    o.isLastJob = FZUtil.getRsString(rs, 13, "");
                }
            }
        }
    }
    public void calcTimes(int startTime, Order o) {
        
        o.jobTiming.calcTime(startTime
            , o.runInput
            , o.readyTime
            , o.millLoc.lon
            , o.millLoc.lat
            , o.block1Loc.lon
            , o.block1Loc.lat
            );

    }

    public boolean calcIsDue(Order o, CalibContext cx) throws Exception {
        
        // get cur time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time1 = sdf.format(new Date());
        String[] te = time1.split(":");
        int hh = Integer.parseInt(te[0]);
        int mm = Integer.parseInt(te[1]);
        int curTime = (hh * 60) + mm;

        // calc other job milestone times if depart now
        //calcTimes(curTime + 5, o);
        o.jobTiming.calcTime(curTime + 5
            , o.runInput
            , o.readyTime
            , o.millLoc.lon
            , o.millLoc.lat
            , o.block1Loc.lon
            , o.block1Loc.lat
            );
        
        // calc latest depart to block
        int durToBlock = o.jobTiming.tripMin + o.runInput.durToUnloadInBlock;
        int latestDepart = o.readyTime - durToBlock;
        int bufferMin = 10;
        latestDepart -= bufferMin;
        
        // log
        logIt(cx,
                "Calc Job: " + o.jobID
                + "\n<br>Bin ready Time: " + FZUtil.toClock(o.readyTime)
                + "\n<br>Cur time: " + FZUtil.toClock(curTime)
                + "\n<br>Total time to block: " + durToBlock
                + "\n<br>Trip to block: " + o.jobTiming.tripMin
                + "\n<br>Unload in block: " + o.runInput.durToUnloadInBlock
                + "\n<br>Latest depart - " + bufferMin + " min buffer: " 
                        + FZUtil.toClock(latestDepart)
        );
        
        // if current time later than latest depart with 5 min buffer
        if (curTime >= latestDepart){
                        
            logIt(cx, "Job is due for assignment:" + o.jobID);
            logIt(cx, "StartTime " + FZUtil.toClock(o.jobTiming.startTime));
            logIt(cx, "ArrvAtBlock " + FZUtil.toClock(o.jobTiming.timeArriveAtDemand));
            logIt(cx, "DepartToMill " + FZUtil.toClock(o.jobTiming.timeDepartToMill));
            logIt(cx, "EndTime " + FZUtil.toClock(o.jobTiming.endTime));

            return true;
        }
        else {
            
            logIt(cx, "Job NOT yet due for assignment:" + o.jobID);
            
            return false;
        }
    }

    void loadVehicles(CalibContext cx) throws Exception {
        
        cx.vehicles = new ArrayList<Vehicle>();
        
        String sql = 

            "select"
            + "\n	v.vehicleID"
            + "\n 	, v.lastRunID "
            + "\n 	, vehJobCount.jobCount"
            + "\n	, vehLastJob.DoneStatus lastJobStatus"
            + "\n 	, vehMaxJob.maxJobID"
            + "\n	, vehLastTask.ActualEnd"
            + "\n	, v.canGoHome"
            + "\n from"
            + "\n 	fbVehicle v "


            + "\n 		left outer join ("
            + "\n 			select "
            + "\n 				ActualTruckID"
            + "\n 				, count(jobID) jobCount"
            + "\n 			from fbjob"
//            + "\n 			where hvsDt >= subdate(curdate(), 1)"
            + "\n 			where hvsDt >= curdate()"
            + "\n 				and doneStatus in ('DONE')"
            + "\n 				and reorderToJobID is null"
            + "\n 			group by actualTruckID "
            + "\n 		) vehJobCount on"
            + "\n 			v.vehicleID = vehJobCount.actualTruckID"


            + "\n 		left outer join ("
            + "\n 			select "
            + "\n 				ActualTruckID"
            + "\n 				, max(jobID) maxJobID"
            + "\n 			from fbjob"
            + "\n 			where hvsDt >= curdate()"
            + "\n 				and doneStatus <> 'PLAN'"
            + "\n 				and reorderToJobID is null"
            + "\n 			group by actualTruckID "
            + "\n 		) vehMaxJob on"
            + "\n 			v.vehicleID = vehMaxJob.actualTruckID"


            + "\n 		left outer join fbJob vehLastJob"
            + "\n 			on vehMaxJob.maxJobID = vehLastJob.jobID"

            + "\n               left outer join fbTask2 vehLastTask"
            + "\n                       on vehLastJob.jobID = vehLastTask.jobID"
            + "\n                           and vehLastTask.taskSeq = 2"


            + "\n where"
            + "\n 	v.includeInRun = 'YES'"
            + "\n 	and left(v.lastRunID,8) = "
            + "\n               date_format(curdate(), '%Y%m%d')"
//            + "\n 	and (vehLastJob.doneStatus in ('DONE')"
//            + "\n           or vehLastJob.doneStatus is null)"


            + "\n order by"
            + "\n 	vehJobCount.jobCount asc"
            + "\n 	, vehLastTask.actualEnd asc"
            ;
        logIt(cx, "Get all vehicles: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            // query
            try (ResultSet rs = ps.executeQuery()){
                
                while (rs.next()){
                    
                    Vehicle v = new Vehicle();
                    v.vehicleID = FZUtil.getRsString(rs, 1, "");
                    v.lastRunID = FZUtil.getRsString(rs, 2, "");
                    v.jobCount = FZUtil.getRsInt(rs, 3, 0);
                    v.lastJobStatus = FZUtil.getRsString(rs, 4, "");
                    v.lastJobID = FZUtil.getRsString(rs, 5, "");
                    v.canGoHome = FZUtil.getRsInt(rs, 6, 0);
                    cx.vehicles.add(v);
                    
                    logIt(cx, "Got vehicle " + v.vehicleID 
                            + ", jobCount = " + v.jobCount
                            + ", lastRunID = " + v.lastRunID
                            + ", lastJobStatus = " + v.lastJobStatus
                            + ", lastJobID = " + v.lastJobID
                    );
                }
            }
        }
    }

    public Vehicle get1AvailVehicle(Order o, CalibContext cx) throws Exception {
        
        for (Vehicle v : cx.vehicles){
            
            boolean isAvail = v.status.equals("AVLB");
            boolean isSameRun = v.lastRunID.equals(o.runID);
            boolean isLastJobDoneOrNull 
                = v.lastJobStatus.equals("DONE") 
                    || v.lastJobStatus.equals("");
            
            if ((isAvail) && (isSameRun) 
                && (isLastJobDoneOrNull) && (v.canGoHome != 1)){

                logIt(cx, "Got avail vehicle = " + v.vehicleID 
                        + ", isAvail/NotAsgn = " + isAvail
                        + ", isSameRun = " + isSameRun
                        + ", isLastJobDoneOrNull = " + isLastJobDoneOrNull
                        + ", canGoHome = " + v.canGoHome
                );

                return v;
            }
            else {
            }
        }
        return null;
    }

    public void logIt(CalibContext cx, String m) throws Exception {
        cx.fullLog.append("\n<br><font color=");
        if (m.toLowerCase().startsWith("err")){
            cx.fullLog.append("red");
        }
        else {
            cx.fullLog.append("black");
        }
        cx.fullLog.append(">---&nbsp").append(m).append("</font>");
        
        FZUtil.logSlowly(cx.drive + ":" + cx.logFolder, cx.appName, m);
    }

    public void cleanUpLog(CalibContext cx) throws Exception {

        logIt(cx, "Clean up log when time");

        // check if non busy time
        Calendar cal = Calendar.getInstance();
        int timeOfDay = cal.get(Calendar.HOUR_OF_DAY);
        if(timeOfDay >= 0 && timeOfDay < 4){

            // init folder loc
            File folder = new File(cx.drive + ":" + cx.logFolder);

            // get last x days string
            cal.add(Calendar.DAY_OF_YEAR, -6);
            String timeStMsg = 
                    (new SimpleDateFormat("yyyyMMdd_HH").format(
                        cal.getTime()));

            // list file
            for (File f : folder.listFiles()) {

                // get file age
                long diff = new Date().getTime() - f.lastModified();
                boolean olderThenXDays = diff > 6 * 24 * 60 * 60 * 1000;

                // if old enough & name matches
                String fileName = f.getName();
                if (fileName.startsWith("log_" + cx.appName + "_" + timeStMsg) 
                    && olderThenXDays) {

                    // delete
                    logIt(cx, "Deleting: " + fileName);
                    f.delete();
                }
            }
        }
    }
%>
<%=run(request, response, pageContext)%>