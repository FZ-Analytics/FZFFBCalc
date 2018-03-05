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
class FB2HarvestDivision {
    public String divisionName = "";
    public FB2Location centeroid = null;
    public String divID = "";
    public double demandSize = 0;
    public FB2Context cx = null;
    public ArrayList<FB2Block> blocks = new ArrayList<FB2Block>();
    public FB2Location millLoc = null;
    public int prodAstUserID;
    public String remark;

    private double getMaxY(double maxY1, FB2Block block) {
        if (block.y2 > maxY1) {
            maxY1 = block.y2;
        }
        return maxY1;
    }
    
    public void calcCenteroid() {
        
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -99999;
        double maxY1 = -99999;
        
        for (FB2Block block : blocks){
            
            if (minX > block.x1) {
                minX = block.x1;
            }
            if (minY > block.y1) {
                minY = block.y1;
            }
            if (maxX < block.x2) {
                maxX = block.x2;
            }
            // there is bug in netbeans, so have to call function
            maxY1 = getMaxY(maxY1, block);
        }
        centeroid = new FB2Location();
        centeroid.lon = minX + ((maxX - minX) / 2);
        centeroid.lat = minY + ((maxY1 - minY) / 2);
        centeroid.name = this.divID;
    }

    void createDemands_ORIG() {
        
        // create demands list
        int binTrips = 1 + (int) (this.demandSize / cx.runInput.binCapacity);
        int curDemandTime = cx.runInput.startFruitReadyForGrabber; // + FBContext.timeToFillBin;
        
        // for each trip
        double remainingSize = this.demandSize;
        for (int binTripID = 1; binTripID <= binTrips; binTripID++){
            
            // calc demand time
            curDemandTime += cx.runInput.durToFillBin;
            
            // calc demand size
            double curDemandSize = 0;
            if (remainingSize < cx.runInput.binCapacity){
                curDemandSize = remainingSize;
            }
            else {
                curDemandSize = cx.runInput.binCapacity;
            }
            remainingSize -= curDemandSize;

            // add demand
            FB2Demand demand = new FB2Demand();
            demand.division = this;
            demand.size = curDemandSize;
            demand.dueTime = curDemandTime;
            demand.demandSeq = binTripID;
            demand.demandID = demand.getDemandID();
            cx.demands.add(demand);
            
        }
    }
    
}
