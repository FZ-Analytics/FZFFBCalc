/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fz.util;

import java.util.HashMap;

/**
 *
 */
public class EObject {
    
    HashMap<String, Object> m = new HashMap<String, Object>();
    
    public String getStr(String name) throws Exception {
        return (String) m.get(name);
    }
    public int getInt(String name) throws Exception {
        return ((Integer) m.get(name)).intValue();
    }
    public double getDbl(String name) throws Exception {
        return ((Double) m.get(name)).doubleValue();
    }
    public Object getObj(String name) throws Exception {
        return m.get(name);
    }
    public void putStr(String name, String val) throws Exception {
        m.put(name, val);
    }
    public void putInt(String name, int val) throws Exception {
        m.put(name, new Integer(val));
    }
    public void putDbl(String name, double val) throws Exception {
        m.put(name, new Double(val));
    }
    public void putObj(String name, Object val) throws Exception {
        m.put(name, val);
    }
}
