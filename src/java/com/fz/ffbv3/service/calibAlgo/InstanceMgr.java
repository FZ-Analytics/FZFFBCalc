/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.calibAlgo;

import com.fz.util.FZUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 */
class InstanceMgr {

//    public static boolean otherIsRunning(CalibContext cx) 
//        throws Exception {
//        
//        boolean isRunning = false;
//        String sql = "select 1 from fbAIRun";
//        cx.log("Check other instance: " + sql);
//        try (PreparedStatement ps = cx.con.prepareStatement(sql);
//                ResultSet rs = ps.executeQuery()){
//            if (rs.next()){
//                isRunning = true;
//            }
//        }
//        return isRunning;
//    }
//
//    public static void removeInstance(CalibContext cx) throws Exception {
//        String sql = "delete from fbAIRun";
//        cx.log("Remove instance: " + sql);
//        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
//            ps.executeUpdate();
//        }
//    }
//
//    public static void addInstance(CalibContext cx) throws Exception {
//        String sql = "insert into fbAIRun(updDt) values(?)";
//        cx.log("Add instance: " + sql);
//        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
//            ps.setTimestamp(1, FZUtil.getCurSQLTimeStamp());
//            ps.executeUpdate();
//        }
//    }
//
}
