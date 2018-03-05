/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.generic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 *
 */
public interface BusinessLogic {

    public abstract void run(
            HttpServletRequest request
            , HttpServletResponse response
            , PageContext pc) 
            throws Exception;

}
