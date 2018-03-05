/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

/**
 *
 * @author Eri Fizal
 */
public class FB2Block {

    String blockID;
    double demandSize;
    double x1;
    double y1;
    double x2;
    double y2;

    FB2Location getCenteroid() {
        double cx = x1 + ((x2-x1)/2);
        double cy = y1 + ((y2-y1)/2);
        FB2Location c = new FB2Location();
        c.lon = cx;
        c.lat = cy;
        c.name = blockID;
        return c;
    }
    
}
