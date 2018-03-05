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
    String drive = null;
    
    public String run(HttpServletRequest request, HttpServletResponse response
            , PageContext pc) throws Exception {

        CalibContext cx = new CalibContext();
        try {
            
            // get passes parameter
            cx.log("Run");
            drive = request.getParameter("drive");
            if (drive == null) {
                drive = "C";
            }
            cx.log("Drive captured = " + drive);
            
            // connect to db
            cx.log("Connecting to db");
            try (Connection con = (new Db()).getConnection("jdbc/fz");){

                // keep connection object
                cx.log("DB Connected");
                cx.con = con;
                
                // check if other instance running
                if (!otherIsRunning(cx)){
                    
                    // add instance
                    addInstance(cx);
                    
                    // run calc
                    cx.log("Calc run");
                    runCalc(cx);
                    
                    // remove instance
                    removeInstance(cx);
                    cx.log("Algo finish");
                }
                else {
                    // other is run
                    cx.log("Other already running");
                }

            }

        } catch (Exception e){
            
            // log
            String m = FZUtil.toStackTraceText(e);
            try {
                // attempt to clear instance
                cx.log("ERROR:\n" + m);
                try (Connection con = (new Db()).getConnection("jdbc/fz");){
                    removeInstance(cx);
                }
                
            } catch (Exception e1){
            }
        }
        
        // return logs
        String return1 = cx.msgLogger.fullLog.toString();
        return return1;
    }

    public  boolean otherIsRunning(CalibContext cx) 
        throws Exception {
        
        boolean isRunning = false;
        String sql = "select 1 from fbAIRun";
        cx.log("Check other instance: " + sql);
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
        cx.log("Remove instance: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
    }

    public void addInstance(CalibContext cx) throws Exception {
        String sql = "insert into fbAIRun(updDt) values(?)";
        cx.log("Add instance: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.setTimestamp(1, FZUtil.getCurSQLTimeStamp());
            ps.executeUpdate();
        }
    }

    void runCalc(CalibContext cx) throws Exception {
        
        // get runID where today & execStatus = INPG
        List<String> runIDs = getINPGRunIDs(cx);
        
        // for each runID
        cx.log("RunIDs to process = " + runIDs.size());
        for (String runID : runIDs){

            cx.log("Current runID " + runID);
            
            // get order list
            //OrderManager om = new OrderManager();
            loadOrders(cx, runID);
            cx.log("Orders due = " + cx.orders.size());
            if (cx.orders.isEmpty()) {
                continue;
            }
            
            // get vehicle list, with total job, total dur
            //VehicleMgr vm = new VehicleMgr();
            loadVehicles(cx);
            cx.log("Vehicles avail = " + cx.vehicles.size());
            if (cx.vehicles.isEmpty()) {
                continue;
            }

            // sort toProcessOrder by dur, small to large
            // sort vehicle by total job, small to large
            // for each toProcessOrder
            for (Order o : cx.orders){

                cx.log("Finding vehicle for order/job " + o.jobID);
                
                // get 1 vehicle
                Vehicle v = get1AvailVehicle(o, cx);

                // if exist
                if (v != null){

                    cx.log("Got avail vehicle " + v.vehicleID);
                    
                    // assign
                    assign(cx, o, v);
                }
                // else, break, no more vehicle
                else {
                    
                    cx.log("No more avail vehicle ");
                    break;
                } 
            }
        }

        // calibrate the configuration to reach optimum level
        CalibAlgo.runArtificialIntelligence(cx);

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
        cx.log("Get job seq: " + sql);
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
        cx.log("Update job: " + sql);
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
        cx.log("Insert tasks: " + sql);
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
        cx.log("Update vehicle: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            ps.executeUpdate();
        }
        
        cx.log("Commit");
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
        cx.log("Get runIDs to process: " + sql);
        try (PreparedStatement ps = cx.con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()){
            while (rs.next()){
                
                String runID = rs.getString(1); 
                runIDs.add(runID);
                
                cx.log("Got runID " + runID);
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
        
        cx.log("Get orders: " + sql);
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
                    
                    cx.log("Got job order: " + o.jobID 
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

//        o.jobTiming.startTime = startTime;
//        
//        // get current time in int
//        double distMtr = FZUtil.calcMeterDist(
//                o.millLoc.lon
//                , o.millLoc.lat
//                , o.block1Loc.lon
//                , o.block1Loc.lat
//        );
//        int tripMin = FZUtil.calcTripMinutes(distMtr
//                , o.runInput.speedKmPHr);
//        tripMin = (int) Math.floor(((double) tripMin) * 1.1);
//        
//        o.jobTiming.tripMillToBlock = tripMin + o.runInput.durToUnloadInBlock;
//        o.timeArriveAtDemand = o.jobTiming.startTime 
//            + tripMin
//            + o.runInput.durToUnloadInBlock
//            ;
//        
//        // calc waiting demand
//        int durWaitingDemand = o.readyTime - o.jobTiming.timeArriveAtDemand;
//        if (durWaitingDemand < 0) durWaitingDemand = 0;
//        
//        // calc depart to mill
//        o.jobTiming.timeDepartToMill = o.jobTiming.timeArriveAtDemand 
//                + durWaitingDemand
//                + o.runInput.durToLoadBinToVehicle;
//        
//        // calc trip to mill
//        int timeArriveAtMill = o.jobTiming.timeDepartToMill + tripMin;
//        
//        // calc end time
//        o.jobTiming.endTime = timeArriveAtMill 
//                + o.runInput.durWaitingBridge
//                + o.runInput.durToWeight
//                + o.runInput.durToUnloadInMill
//                ;
        
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
        
//        // get trip min
//        double distMtr = FZUtil.calcMeterDist(
//                o.millLoc.lon
//                , o.millLoc.lat
//                , o.block1Loc.lon
//                , o.block1Loc.lat
//        );
//        int tripMin = FZUtil.calcTripMinutes(distMtr
//                , o.runInput.speedKmPHr);
//        tripMin = (int) Math.floor(((double) tripMin) * 1.1);
//        o.tripMillToBlock = tripMin + o.runInput.durToUnloadInBlock;
        
        // calc latest depart to block
        int durToBlock = o.jobTiming.tripMin + o.runInput.durToUnloadInBlock;
        int latestDepart = o.readyTime - durToBlock;
        int bufferMin = 10;
        latestDepart -= bufferMin;
        
        // log
        cx.log(
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
                        
            cx.log("Job is due for assignment:" + o.jobID);
            cx.log("StartTime " + FZUtil.toClock(o.jobTiming.startTime));
            cx.log("ArrvAtBlock " + FZUtil.toClock(o.jobTiming.timeArriveAtDemand));
            cx.log("DepartToMill " + FZUtil.toClock(o.jobTiming.timeDepartToMill));
            cx.log("EndTime " + FZUtil.toClock(o.jobTiming.endTime));

            return true;
        }
        else {
            
            cx.log("Job NOT yet due for assignment:" + o.jobID);
            
            return false;
        }
    }

    void loadVehicles(CalibContext cx) throws Exception {
        
        cx.vehicles = new ArrayList<Vehicle>();
        
//        String sql = 
//            "select"
//            + "\n	v.vehicleID"
//            + "\n 	, v.lastRunID "
//            + "\n 	, vehJobCount.jobCount"
//            + "\n	, vehLastJob.DoneStatus lastJobStatus"
//            + "\n 	, vehMaxJob.maxJobID"
//            + "\n	, vehLastTask.ActualEnd"
//            + "\n from"
//            + "\n 	fbVehicle v "
//            + "\n 		left outer join ("
//            + "\n 			select "
//            + "\n 				ActualTruckID"
//            + "\n 				, count(jobID) jobCount"
//            + "\n 			from fbjob"
//            + "\n 			where hvsDt >= subdate(curdate(), 1)"
//            + "\n 				and doneStatus in ('DONE')"
//            + "\n 				and reorderToJobID is null"
//            + "\n 			group by actualTruckID "
//            + "\n 		) vehJobCount on"
//            + "\n 			v.vehicleID = vehJobCount.actualTruckID"
//            + "\n 		left outer join ("
//            + "\n 			select "
//            + "\n 				max(jobID) maxJobID"
//            + "\n 			from fbjob"
//            + "\n 			where hvsDt >= curdate()"
//            + "\n 				and (doneStatus in ('DONE') or doneStatus is null)"
//            + "\n 				and reorderToJobID is null"
//            + "\n 			group by actualTruckID "
//            + "\n 		) vehMaxJob on"
//            + "\n 			v.vehicleID = vehJobCount.actualTruckID"
//            + "\n 		left outer join fbJob vehLastJob"
//            + "\n 			on vehMaxJob.maxJobID = vehLastJob.jobID"
//            + "\n               left outer join fbTask2 vehLastTask"
//            + "\n                       on vehLastJob.jobID = vehLastTask.jobID"
//            + "\n                           and vehLastTask.taskSeq = 2"
//            + "\n where"
//            + "\n 	v.includeInRun = 'YES'"
//            + "\n 	and left(v.lastRunID,8) = "
//            + "\n               date_format(curdate(), '%Y%m%d')"
//            + "\n 	and (vehLastJob.doneStatus in ('DONE')"
//            + "\n           or vehLastJob.doneStatus is null)"
//            + "\n order by"
//            + "\n 	vehJobCount.jobCount asc"
//            + "\n 	, vehLastTask.actualEnd asc"
//            ;
        String sql = 

            "select"
            + "\n	v.vehicleID"
            + "\n 	, v.lastRunID "
            + "\n 	, vehJobCount.jobCount"
            + "\n	, vehLastJob.DoneStatus lastJobStatus"
            + "\n 	, vehMaxJob.maxJobID"
            + "\n	, vehLastTask.ActualEnd"
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
            + "\n 	and (vehLastJob.doneStatus in ('DONE')"
            + "\n           or vehLastJob.doneStatus is null)"


            + "\n order by"
            + "\n 	vehJobCount.jobCount asc"
            + "\n 	, vehLastTask.actualEnd asc"
            ;
        cx.log("Get all avail vehicles: " + sql);
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
                    cx.vehicles.add(v);
                    
                    cx.log("Got vehicle " + v.vehicleID 
                            + ", jobCount = " + v.jobCount
                            + ", lastRunID = " + v.lastRunID
                            + ", lastJobStatus = " + v.lastJobStatus
                            + ", lastJobID = " + v.lastJobID
                    );
                }
            }
        }
    }

    public Vehicle get1AvailVehicle(Order o, CalibContext cx) {
        
        for (Vehicle v : cx.vehicles){
            
            boolean isAvail = v.status.equals("AVLB");
            boolean isSameRun = v.lastRunID.equals(o.runID);
            
            if ((isAvail) && (isSameRun)){
                return v;
            }
        }
        return null;
    }

%>
<%=run(request, response, pageContext)%>