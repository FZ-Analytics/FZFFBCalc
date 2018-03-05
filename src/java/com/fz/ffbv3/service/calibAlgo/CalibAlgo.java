/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.calibAlgo;

import com.fz.util.FZUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class CalibAlgo {

//    static void run_OLD(Connection con) throws Exception {
//        
//        CalibContext cx = new CalibContext();
//        cx.con = con;
//        
//        // get order list
//        OrderManager om = new OrderManager();
//        om.loadOrders(cx);
//        
//        // get vehicle list, with total job, total dur
//        VehicleMgr vm = new VehicleMgr();
//        vm.loadVehicles(cx);
//        
//        // sort toProcessOrder by dur, small to large
//        // sort vehicle by total job, small to large
//        // for each toProcessOrder
//        for (Order o : cx.orders){
//            
//            // get 1 vehicle
//            Vehicle v = vm.get1AvailVehicle(o.divID);
//            
//            // if exist
//            if (v != null){
//                
//                // assign
//                JobAssigner.assign(cx, o, v);
//            }
//            // else, break, no more vehicle
//            else break;
//        }
//    }

//    static void run(CalibContext cx) throws Exception {
//        
//        // get runID where today & execStatus = INPG
//        List<String> runIDs = RunIDsGetter.getINPGRunIDs(cx);
//        
//        // for each runID
//        cx.log("RunIDs to process = " + runIDs.size());
//        for (String runID : runIDs){
//
//            cx.log("Current runID " + runID);
//            
//            // get order list
//            OrderManager om = new OrderManager();
//            om.loadOrders(cx, runID);
//            cx.log("Orders due = " + cx.orders.size());
//            if (cx.orders.isEmpty()) {
//                continue;
//            }
//            
//            // get vehicle list, with total job, total dur
//            VehicleMgr vm = new VehicleMgr();
//            vm.loadVehicles(cx);
//            cx.log("Vehicles avail = " + cx.vehicles.size());
//            if (cx.vehicles.isEmpty()) {
//                continue;
//            }
//
//            // sort toProcessOrder by dur, small to large
//            // sort vehicle by total job, small to large
//            // for each toProcessOrder
//            for (Order o : cx.orders){
//
//                cx.log("Finding vehicle for order/job " + o.jobID);
//                
//                // get 1 vehicle
//                Vehicle v = vm.get1AvailVehicle(o);
//
//                // if exist
//                if (v != null){
//
//                    cx.log("Got avail vehicle " + v.vehicleID);
//                    
//                    // assign
//                    JobAssigner.assign(cx, o, v);
//                }
//                // else, break, no more vehicle
//                else {
//                    
//                    cx.log("No more avail vehicle ");
//                    break;
//                } 
//            }
//        }
//    }
    public static void runArtificialIntelligence(CalibContext cx) 
            throws Exception {
        // TODO:
    }
}
