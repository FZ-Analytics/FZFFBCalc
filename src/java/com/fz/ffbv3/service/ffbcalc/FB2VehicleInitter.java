/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.util.FZUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author Eri Fizal
 */
public class FB2VehicleInitter {

    public static void initVehicles(FB2Context cx) throws Exception{
        
        StringBuffer vehicleIDList = new StringBuffer();
        for (String vehicleID : cx.runInput.vehicleList){
            
            if (vehicleIDList.length() > 0)
                vehicleIDList.append(",");
            
            vehicleIDList.append("'" + vehicleID + "'");
            
        }
        
        String sql = "select"
                + "\n vehicleID"
                + "\n, weight"
                + "\n, type"
                + "\n, startTime"
                + "\n, defDivCode"
                + "\n, defDriverID"
                + "\n, startLon"
                + "\n, startLat"
                + "\n, startLocation"
                + "\n, vehicleName"
                + "\n from fbVehicle"
                + "\n where vehicleID in (" + vehicleIDList + ")"
                ;
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            try (ResultSet rs = ps.executeQuery()){
                
                while (rs.next()){
                    
                    // add vehicle
                    FB2Vehicle veh = new FB2Vehicle(cx);
                    veh.vehicleID = rs.getString(1);
                    veh.capacity = FZUtil.getRsDoubleErr(rs,2,"Invalid vehicle capacity, vehicleID " + veh.vehicleID);
                    veh.type = rs.getString(3);
                    veh.earliestDepart = FZUtil.clockToMin(rs.getString(4));
                    veh.defaultDivID = rs.getString(5);

                    // loc and movement
                    FB2Location loc = new FB2Location();
                    loc.lon = FZUtil.getRsDoubleErr(rs,7,"Invalid vehicle startLon, vehicleID " + veh.vehicleID);
                    loc.lat = FZUtil.getRsDoubleErr(rs,8,"Invalid vehicle startLat, vehicleID " + veh.vehicleID);
                    loc.name = rs.getString(9);
                    veh.startLoc = loc; 

                    veh.vehicleName = rs.getString(10);
                    
                    cx.vehicles.add(veh);
                }
            }
        }
        if (cx.vehicles.isEmpty())
            throw new Exception("No vehicle retrieved with ID: " + vehicleIDList);
        
        // shuffle so it is random
        Collections.shuffle(cx.vehicles);
    }
    
}
