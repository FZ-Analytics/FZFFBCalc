/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fz.ffbv3.service.ffbcalc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author Eri Fizal
 */
class FB2SorterOfDemandDue {

    static void sortByDemandDue(ArrayList<FB2Job> jobs) {

        Collections.sort(jobs, new Comparator() {

            public int compare(Object o1, Object o2) {

                Integer x1 = ((FB2Job) o1).demand.dueTime;
                Integer x2 = ((FB2Job) o2).demand.dueTime;
                
                // sort low to high?
                int sComp = x1.compareTo(x2);
                return sComp;
                
        }});
    }
    
}
