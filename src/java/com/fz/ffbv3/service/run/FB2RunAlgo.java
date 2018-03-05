/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.ffbv3.service.run;

import com.fz.ffbv3.service.ffbcalc.FB2OptimizerThread;
import com.fz.ffbv3.service.ffbcalc.FB2RunInput;
import com.fz.generic.Db;
import com.fz.util.FZUtil;
import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 *
 */
public class FB2RunAlgo  {

    public String run(HttpServletRequest request, HttpServletResponse response
            , PageContext pc) throws Exception {
        
        String runID = FZUtil.getHttpParam(request, "runID");
        
        // run it
        FB2OptimizerThread o1 = 
                new FB2OptimizerThread(runID);
        o1.start();
        
        // redirect to progress page
        return "OK";
    }

}
