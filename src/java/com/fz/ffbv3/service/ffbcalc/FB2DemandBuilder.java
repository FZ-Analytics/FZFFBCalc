/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import com.fz.util.FZUtil;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 *
 */
public class FB2DemandBuilder {
    
    //FB2Context cx = null;
    
    public void buildDemands(FB2Context cx) throws Exception {
        
        // create sql param of div id list
        StringBuffer divIDList = new StringBuffer();
        for (String divID : cx.runInput.divList){
            
            if (divIDList.length() > 0)
                divIDList.append(",");
            
            divIDList.append("'").append(divID).append("'");
        }            
        
        // create the sql
        String sql = "select "
                + "\n he.divID"
                + "\n, hed.block"
                + "\n, hed.size1"
                + "\n, hed.taskType"
                + "\n, b.x1"
                + "\n, b.y1"
                + "\n, b.x2"
                + "\n, b.y2"
                + "\n, m.millID"
                + "\n, m.lon"
                + "\n, m.lat"
                + "\n, d.prodAstUserID"
                + "\n, he.remark"
                + "\n from fbHvsEstm he"
                + "\n   left outer join fbHvsEstmDtl hed"
                + "\n         on he.hvsEstmID = hed.hvsEstmID"
                + "\n   left outer join fbBlock b"
                + "\n         on hed.block = b.blockID"
                + "\n         and he.divID = b.divID"
                + "\n   left outer join fbDiv d"
                + "\n         on d.divID = he.divID"
                + "\n   left outer join fbMill m"
                + "\n         on d.millID = m.millID"
                + "\n where he.hvsDt = ?"
                + "\n   and he.divID in (" + divIDList.toString() + ")"
                + "\n order by "
                + "\n   he.divID"
                + "\n   , hed.estmSeq"
                + "\n   , hed.block"
                ;
        
        // get data
        try (PreparedStatement ps = cx.con.prepareStatement(sql)){
            
            ps.setTimestamp(1, FZUtil.toSQLTimeStamp(cx.runInput.hvsDate, "yyyy-MM-dd"));
            
            try (ResultSet rs = ps.executeQuery()){
                
                while (rs.next()){
                    
                    // add block 
                    String divID = rs.getString(1);
                    
                    FB2Block block = new FB2Block();
                    block.blockID = rs.getString(2);
                    block.demandSize = FZUtil.getRsDoubleErr(rs,3,"Invalid estm size for " + divID + " - " + block.blockID);

                    block.x1 = FZUtil.getRsDoubleErr(rs,5,"Invalid x1 for " + divID + " - " + block.blockID);
                    block.y1 = FZUtil.getRsDoubleErr(rs,6,"Invalid y1 for " + divID + " - " + block.blockID);
                    block.x2 = FZUtil.getRsDoubleErr(rs,7,"Invalid x2 for " + divID + " - " + block.blockID);
                    block.y2 = FZUtil.getRsDoubleErr(rs,8,"Invalid y1 for " + divID + " - " + block.blockID);

                    String millID = rs.getString(9);
                    double millLon = FZUtil.getRsDoubleErr(rs,10,"Invalid millLon for " + divID + " - " + block.blockID);
                    double millLat = FZUtil.getRsDoubleErr(rs,11,"Invalid millLat for " + divID + " - " + block.blockID);
                    int prodAstUserID = (int) FZUtil.getRsDoubleErr(
                            rs,12,"Invalid ProdAstUserID for div " + divID);

                    String remark = FZUtil.getRsString(rs,13,"");

                    // check if this is new div
                    FB2HarvestDivision div = null;
                    for (FB2HarvestDivision idiv : cx.divisions){
                        if (idiv.divID.equals(divID)) {
                            div = idiv;
                            break;
                        }
                    }
                    // if new div, add to div list
                    if (div == null) {
                        div = new FB2HarvestDivision();
                        div.divID = divID;
                        div.cx = cx;
                        div.prodAstUserID = prodAstUserID;
                        div.remark = remark;
                        cx.divisions.add(div);
                    }

                    // set block division
                    div.blocks.add(block);

                    // add div demandSize
                    div.demandSize += block.demandSize;
                    cx.totalDemandSize += block.demandSize;

                    // add mill loc
                    div.millLoc = new FB2Location();
                    div.millLoc.name = millID;
                    div.millLoc.lon = millLon;
                    div.millLoc.lat = millLat;
                }

                // calc centeroid lon lat
                for (FB2HarvestDivision div : cx.divisions) {
                    div.calcCenteroid();
                }

                createDemands(cx);
            
            }
        }
        
    }
    
    void createDemands(FB2Context cx){
        
        // for each div
        for (FB2HarvestDivision dv : cx.divisions){
            
            int binTripID = 0;
            int curDemandTime = cx.runInput.startFruitReadyForGrabber;
            
            double curBinSize = 0;
            StringBuffer curBinBlocks = new StringBuffer();
            FB2Location curBinLoc = null;

            // for each block
            for (FB2Block b : dv.blocks){
            
                if (curBinBlocks.length() == 0){
                    curBinLoc = b.getCenteroid();
                }
                else {
                    curBinBlocks.append(",");
                }
                curBinBlocks.append(b.blockID);
                
                // calc demand sizes
                double rem = b.demandSize;
                
                // while rem > 0
                while (rem > 0){
                    
                    // if rem fit
                    if (rem < (cx.runInput.binCapacity - curBinSize)){
                        
                        // add & break
                        curBinSize += rem;
                        break;
                    }
                    // else, not fit
                    else {
                        // rem =- fitAmt
                        rem = rem - (cx.runInput.binCapacity - curBinSize);
                        
                        // close bin
                        FB2Demand demand = new FB2Demand();
                        demand.division = dv;
                        demand.size = cx.runInput.binCapacity; // full
                        curDemandTime = curDemandTime 
                                + cx.runInput.durToFillBin;
                        demand.dueTime = curDemandTime;
                        demand.demandSeq = ++binTripID;
                        demand.demandID = demand.getDemandID();
                        demand.binBlocks = curBinBlocks.toString();
                        demand.loc = curBinLoc;
                        cx.demands.add(demand);
                        
                        // open new
                        curBinSize = 0;
                        curBinBlocks.setLength(0);
                        curBinBlocks.append(b.blockID);
                        curBinLoc = b.getCenteroid();
                    }
                }
                    
            }
            
            // add remaining
            if (curBinSize > 0){
                
                // close bin
                FB2Demand demand = new FB2Demand();
                demand.division = dv;
                demand.size = curBinSize; // whatever remaining
                curDemandTime = curDemandTime + cx.runInput.durToFillBin;
                demand.dueTime = curDemandTime;
                demand.demandSeq = ++binTripID;
                demand.demandID = demand.getDemandID();
                demand.binBlocks = curBinBlocks.toString();
                demand.loc = curBinLoc;
                cx.demands.add(demand);
            }
            
        }
    }
    
//    double getDouble(ResultSet rs, int pos, String errMsg) 
//            throws Exception {
//        try {
//            double d = Double.parseDouble(rs.getString(pos));
//            return d;
//        } catch(Exception e){
//            throw new Exception(errMsg);
//        }
//    }
}
