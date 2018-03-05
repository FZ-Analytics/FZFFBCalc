/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.util.FZUtil;
import java.util.ArrayList;

/**
 *
 * @author Eri Fizal
 */
public class FB2Trip {

    static String buildID(FB2Location from1, FB2Location to1) {
        return 
                from1.lon 
                + "," + from1.lat
                + " " + to1.lon 
                + "," + to1.lat
                        ;
    }

    public ArrayList<FB2RoadSegment> roadSegments 
            = new ArrayList<FB2RoadSegment>();
    
    public FB2Location from1 = null;
    public FB2Location to1 = null;
    private FB2Context cx = null;

    FB2Trip(FB2Context cx
            , FB2Location from1, FB2Location to1) throws Exception {
        this.from1 = from1;
        this.to1 = to1;
        this.cx = cx;
        loadPath();
    }
    
    public String getID() {
        return buildID(from1, to1);
    }

    private void loadPath() throws Exception {
        
        // Need to upload once into cost distance matrix
        String sql = "select "
                + "d.fromID"
                + ", d.toID"
                + ", d.roadID"
                + ", d.seq"
                + ", r.x1"
                + ", r.y1"
                + ", r.x2"
                + ", r.y2"
                + " from fbDefinedPath d "
                + " inner join fbRoadSegment r"
                + "  on d.roadID = r.roadID"
                + " where fromID = '" + from1.name + "'" 
                + " or toID = '" + to1.name + "'"
                + " order by seq" 
                ;

        
        ArrayList<String[]> recs = FZUtil.queryToList(
                cx.con, sql);
        for (String[] r : recs){
            
            FB2RoadSegment s = new FB2RoadSegment();
            s.seq = Integer.parseInt(r[3]);
            s.x1 = Double.parseDouble(r[4]);
            s.y1 = Double.parseDouble(r[5]);
            s.x2 = Double.parseDouble(r[6]);
            s.y2 = Double.parseDouble(r[7]);
            s.distanceMtr = FZUtil.calcMeterDist(s.x1, s.y1, s.x2, s.y2);
            s.durationMin = FZUtil.calcTripMinutes(
                    s.distanceMtr, cx.runInput.speedKmPHr) * 2;
            roadSegments.add(s);
            
        }
    }

//    private void loadPath2() throws Exception {
//        
//        // TODO: need to upload once into cost distance matrix
//        String sql = "select "
//                + "d.fromID"
//                + ", d.toID"
//                + ", d.roadID"
//                + ", d.seq"
//                + ", r.x1"
//                + ", r.y1"
//                + ", r.x2"
//                + ", r.y2"
//                + " from fbDefinedPath d "
//                + " inner join fbRoadSegment r"
//                + "  on d.roadID = r.roadID"
//                + " where fromID = '" + from1.name + "'" 
//                + " or toID = '" + to1.name + "'"
//                + " order by seq" 
//                ;
//
//        
//        ArrayList<String[]> recs = FZUtil.queryToList(
//                cx.conMgr.getCon(), sql);
//        for (String[] r : recs){
//            
//            FB2RoadSegment s = new FB2RoadSegment();
//            s.seq = Integer.parseInt(r[3]);
//            s.x1 = Double.parseDouble(r[4]);
//            s.y1 = Double.parseDouble(r[5]);
//            s.x2 = Double.parseDouble(r[6]);
//            s.y2 = Double.parseDouble(r[7]);
//            s.distanceMtr = FZUtil.calcMeterDist(s.x1, s.y1, s.x2, s.y2);
//            s.durationMin = FZUtil.calcTripMinutes(
//                    s.distanceMtr, FB2Context.speedKmPHr) * 2;
//            roadSegments.add(s);
//            
//        }
//    }


    public double getDistanceMtr() {
        double distMtr = 0;
        if (roadSegments.size() == 0 ){
            distMtr = FZUtil.calcMeterDist(
                    from1.lon
                    , from1.lat
                    , to1.lon
                    , to1.lat
            );
        }
        else {
            for (FB2RoadSegment s : roadSegments){
                distMtr += s.distanceMtr;
            }
        }
        return distMtr;
    }

    public int getTripMinutes() {
        int tripMin = 0;
        if (roadSegments.size() == 0 ){
            tripMin = FZUtil.calcTripMinutes(getDistanceMtr()
                    , cx.runInput.speedKmPHr);
            tripMin = (int) Math.floor(((double) tripMin) * 1.1);
        }
        else {
            for (FB2RoadSegment s : roadSegments){
                tripMin += s.durationMin;
            }
        }
        return tripMin;
    }
}

