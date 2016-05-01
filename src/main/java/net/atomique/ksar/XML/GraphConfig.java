/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.atomique.ksar.XML;

import java.util.HashMap;

/**
 *
 * @author Max
 */
public class GraphConfig {

    public GraphConfig(String s1, String s2, String s3) {
        name = s1;
        Title = s2;
        type= s3;
    }
    public String getTitle() {
        return Title;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    
    public void addPlot(PlotConfig s) {
        Plotlist.put(s.getTitle(), s);
    }

    public HashMap<String, PlotConfig> getPlotlist() {
        return Plotlist;
    }

    public void addStack(StackConfig s) {
        Stacklist.put(s.getTitle(), s);
    }

    public HashMap<String, StackConfig> getStacklist() {
        return Stacklist;
    }

    private String name =null;
    private String Title = null;
    private String type = null;
    HashMap<String,PlotConfig> Plotlist = new HashMap<String,PlotConfig>();
    HashMap<String,StackConfig> Stacklist = new HashMap<String,StackConfig>();
}
