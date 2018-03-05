/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Eri Fizal
 */
public class FZUtil {
    
    public static <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

    public static <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
    
//    public static double calcDistance(double x1, double y1
//            , double x2, double y2) {
//        double distance = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
//        return distance;
//    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * 
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double calcMeterDist(double lon1, double lat1, 
            double lon2, double lat2) {
        double el1 = 0; // was in function param
        double el2 = 0; // was in function param
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public static int calcTripMinutes(double distanceMtr, double speedKmPHr){

        // convert speed into meter / minutes
        double speedMtrPMin = speedKmPHr * 1000 / 60; // mt / min

        // calc the time the vehicle need to reach the demand
        int durationMin = (int) Math.ceil(distanceMtr / speedMtrPMin);

        return durationMin;
    }
    
    public static String escapeText(String x){
        String o = x;
        o = o.replaceAll("\\\\","/");
        o = o.replaceAll("\\\"","`");
        o = o.replaceAll("\\'","`");
        o = o.replaceAll("\\t"," ");
        o = o.replaceAll("\\n"," ");
        o = o.replaceAll("\\r"," ");
        o = o.replaceAll("<","(");
        o = o.replaceAll(">",")");    
        return o;
    }
    
    public static boolean isNumeric(String str)  
    {  
      try  
      {  
        double d = Double.parseDouble(str);  
      }  
      catch(NumberFormatException nfe)  
      {  
        return false;  
      }  
      return true;  
    }

    public static String toNumberString(String val, double default1) {
        String result = String.valueOf(default1);
        if (isNumeric(val)) result = val;
        return result;
    }
    
    public static double toDouble(String val, double default1) {
        double d = default1;
        try {
            d = Double.parseDouble(val);
        } catch (Exception e) {
        }
        return d;
    }
    
    // example "yyyy-MM-dd"
    public static String toDateString(Date d, String formatString){
        SimpleDateFormat formatter = new SimpleDateFormat(formatString);
        return formatter.format(d);
    }

    public static int[] splitToComponentTimes(double longVal)
    {
        //long longVal = biggy.longValue();
        int hours = (int) longVal / 3600;
        int remainder = (int) longVal - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        int[] ints = {hours , mins , secs};
        return ints;
    }

    public static String ifMatch(String checkString, String match, String returnWhenMatch){
        String result = checkString;
        if (checkString.equals(match)) result = returnWhenMatch;
        return result;
    }
    
    public static String getTimeID() {
        String id = (new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(
                        new java.util.Date()));
        return id;
    }
    
    public static String getCurTime() {
        String id = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                        new java.util.Date()));
        return id;
    }
    
//    public static ResultSet queryToRecordSet(String sql, Statement st) throws Exception {
//        try {
//            ResultSet rs = st.executeQuery(sql);
//            return rs;
//        }
//        catch (Exception e){
//            throw new Exception(e.getMessage() + " ; SQL = " + sql);
//        }
//    }
    
    public static void queryExecute(Connection con, String sql) 
            throws Exception {
        try {
            
            if (con == null) throw new Exception("Connection is null");
            if (con.isClosed()) throw new Exception("Connection is closed");
            
            try (Statement st = con.createStatement()){
                st.executeUpdate(sql);
            }
//            st.close();
//            st = null;
            
        }
        catch (Exception e){
            throw new Exception(e.getMessage() + " ; SQL = " + sql);
        }
    }
    
    public static ArrayList<String[]> queryToList(Connection con, String sql) 
            throws Exception {
        try {
            
            if (con == null) throw new Exception("Connection is null");
            if (con.isClosed()) throw new Exception("Connection is closed");
            
            ArrayList<String[]> result = new ArrayList<String[]>();
            
            try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
                
                ResultSetMetaData rsm = rs.getMetaData();
                int colCount = rsm.getColumnCount();

                while (rs.next()){
                    String[] flds = new String[colCount];
                    for (int i=0; i< colCount; i++){
                        flds[i] = rs.getString(i+1);
                    }
                    result.add(flds);
                }
            }
//            // clean up
//            rs.close();
//            st.close();
//            rs = null;
//            st = null;
            
            return result;
        }
        catch (Exception e){
            throw new Exception(e.getMessage() + " ; SQL = " + sql);
        }
    }
    
    public static String queryToJSONText(Connection con, String sql)
            throws Exception {
        try {
            if (con == null) throw new Exception("Connection is null");
            if (con.isClosed()) throw new Exception("Connection is closed");
            
            StringBuffer sbField = new StringBuffer();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            
            // create field names
            ResultSetMetaData rsm = rs.getMetaData();
            int colCount = rsm.getColumnCount();
            for (int i=0; i< colCount; i++){
                if (sbField.length() > 0) 
                    sbField.append(",");
                sbField.append("\"" + rsm.getColumnName(i+1) + "\"");
            }
            
            // create rows
            StringBuffer sbRec = new StringBuffer();
            while (rs.next()){
                
                // add comma in between records
                if (sbRec.length() > 0)
                    sbRec.append("\n,");
                
                // start record
                sbRec.append("\n[");
                
                // add fields
                for (int i=0; i< colCount; i++){
                    if (i>0) sbRec.append(",");
                    sbRec.append("\"" + rs.getString(i+1) + "\"");
                }
                
                // close record
                sbRec.append("]");
            }
            
            // clean up
            rs.close();
            st.close();
            rs = null;
            st = null;
            
            // concat fields & records JSON
            StringBuffer sb = new StringBuffer();
            sb
                    .append("{")
                        .append("\n\"fieldNames\":")
                            .append("\n[")
                                .append(sbField)
                            .append("\n]")
                        .append("\n, \n\"records\":")
                            .append("\n[")
                                .append(sbRec)
                            .append("\n]")
                    .append("\n}")
                    ;
            
            return sb.toString();
        }
        catch (Exception e){
            throw new Exception(e.getMessage() + " ; SQL = " + sql);
        }
    }
    
//    public static JSONObject queryToJsonObject(Connection con, String sql)
//            throws Exception {
//        try {
//            if (con == null) throw new Exception("Connection is null");
//            if (con.isClosed()) throw new Exception("Connection is closed");
//            
//            StringBuffer sbField = new StringBuffer();
//            Statement st = con.createStatement();
//            ResultSet rs = st.executeQuery(sql);
//            
//            // create field names
//            ResultSetMetaData rsm = rs.getMetaData();
//            int colCount = rsm.getColumnCount();
//            JSONArray jsonFieldNames = new JSONArray();
//            for (int i=0; i< colCount; i++){
//                jsonFieldNames.put(rsm.getColumnName(i+1));
//            }
//            
//            // create rows
//            JSONArray jsonRecords = new JSONArray();
//            while (rs.next()){
//                
//                // start record
//                JSONArray jsonRecord = new JSONArray();
//                
//                // add fields
//                for (int i=0; i< colCount; i++){
//                    jsonRecord.put(rs.getString(i+1));
//                }
//                
//                // close record
//                jsonRecords.put(jsonRecord);
//            }
//            
//            // clean up
//            rs.close();
//            st.close();
//            rs = null;
//            st = null;
//            
//            // concat fields & records JSON
//            JSONObject jsonAll = new JSONObject();
//            jsonAll.put("fieldsNames", jsonFieldNames);
//            jsonAll.put("records", jsonRecords);
//            return jsonAll;
//        }
//        catch (Exception e){
//            throw new Exception(e.getMessage() + " ; SQL = " + sql);
//        }
//    }
    
    public static String queryToItem(Connection con, String sql
            , String default1) 
            throws Exception {
        try {
            String result = default1;
            ArrayList<String[]> list1 = queryToList(con, sql);
            if (list1.size()>0) {
                result = list1.get(0)[0];
            }
            return result;
        }
        catch (Exception e){
            throw new Exception(e.getMessage() + " ; SQL = " + sql);
        }
    }
    
    public static int queryUpdate(String sql, Statement st) throws Exception {
        try {
            return st.executeUpdate(sql);
        }
        catch (Exception e){
            throw new Exception(e.getMessage() + " ; SQL = " + sql);
        }
    }
    
    public static void logSlowly(String folderPath, String app, String msg)
        throws Exception {
        
        // pad folderPath if needed
        if (!folderPath.endsWith("\\")) folderPath += "\\";
        
        // ensure dir exist
        (new java.io.File(folderPath)).mkdirs();

        // construct log file name
        String timeSt = (new SimpleDateFormat("yyyyMMdd_HH").format(
                    new java.util.Date()));
        String fileName = 
                folderPath 
                + "log_" 
                + app 
                + "_" 
                + timeSt 
                + ".txt"
                ;
        
        // ensure file exist
        File yourFile = new File(fileName);
        yourFile.createNewFile(); // if file already exists will do nothing 
        //FileOutputStream oFile = new FileOutputStream(yourFile, false); 
        
        // add time to msg
        String timeStMsg = 
                (new SimpleDateFormat("\n[yyyy-MM-dd HH:mm:ss] ").format(
                    new java.util.Date()));
        msg = timeStMsg + msg;
        
        // append msg to file
        Files.write(
                Paths.get(fileName)
                , msg.getBytes()
                , StandardOpenOption.APPEND
        );
    }
    
    public static String toStackTraceText(Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String st = sw.toString(); // stack trace as a string   
        return st;
    }
    
    public static String toClock(int minutesFromMidNight){

        String startTime = "00:00";
        int minutes = minutesFromMidNight; //120;
        int h = minutes / 60 + Integer.parseInt(startTime.substring(0,1));
        int m = minutes % 60 + Integer.parseInt(startTime.substring(3,4));
        String newtime = to2Digit(h) + ":" + to2Digit(m);
        return newtime;
    }
    
    public static String to2Digit(int number1){
        return String.format("%02d", number1);
    }
    
    public static int randBetween(int min, int max){
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    public static int clockToMin(String clock){
        String sh = clock.substring(0,2);
        String sm = clock.substring(3,5);
        int h = Integer.parseInt(sh);
        int m = Integer.parseInt(sm);
        int r = (h*60) + m;
        return r;
    }

    public static String getHttpParam(HttpServletRequest request, String paramName) 
            throws Exception {
        String x = request.getParameter(paramName);
        if (x == null) return "";
        return x;
        
    }

    public static String getRequestAttr(HttpServletRequest request, String paramName) 
            throws Exception {
        String x = (String) request.getAttribute(paramName);
        if (x == null) return "";
        return x;
        
    }
    
    public static boolean isValidDate(String dateString) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        try {
            df.parse(dateString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String getRsString(
        ResultSet rs, int pos, String defaultVal) 
            throws Exception {
        String v = rs.getString(pos);
        if (v == null) v = defaultVal;
        if (v.length() == 0) v = defaultVal;
        return v;
    }
    
    public static int getRsInt(
        ResultSet rs, int pos, int defaultVal) 
            throws Exception {
        int v = rs.getInt(pos);
        return (rs.wasNull() ? defaultVal : v);
    }
    
    public static double getRsDouble(
        ResultSet rs, int pos, double defaultVal) 
            throws Exception {
        double v = rs.getDouble(pos);
        return (rs.wasNull() ? defaultVal : v);
    }
    
    public static double getRsDoubleErr(ResultSet rs, int pos, String errMsg) 
            throws Exception {
        try {
            double d = Double.parseDouble(rs.getString(pos));
            return d;
        } catch(Exception e){
            throw new Exception(errMsg);
        }
    }

    public static java.sql.Timestamp toSQLTimeStamp(String dateStr, String formatStr)
        throws Exception {
        java.text.DateFormat format = new java.text.SimpleDateFormat(formatStr);
        java.util.Date date = format.parse(dateStr);
        java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
        return timestamp;
    }
    
    public static java.sql.Timestamp getCurSQLTimeStamp()
        throws Exception {
        java.util.Date today = new java.util.Date();
        java.sql.Timestamp ts = new java.sql.Timestamp(today.getTime());
        return ts;
    }
}
