/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

import java.util.LinkedHashMap;

public class GraphConfig {

    public GraphConfig(String s1, String s2, String s3) {
        name = s1;
        Title = s2;
        type= s3;
        plotlist = new LinkedHashMap<>();
        stacklist = new LinkedHashMap<>();
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
    
    public void addPlot(PlotStackConfig s) {
        plotlist.put(s.getTitle(), s);
    }

    public LinkedHashMap<String, PlotStackConfig> getPlotlist() {
        return plotlist;
    }

    public void addStack(PlotStackConfig s) {
        stacklist.put(s.getTitle(), s);
    }

    public LinkedHashMap<String, PlotStackConfig> getStacklist() {
        return stacklist;
    }

    private String name =null;
    private String Title = null;
    private String type = null;
    private LinkedHashMap<String,PlotStackConfig> plotlist;
    private LinkedHashMap<String,PlotStackConfig> stacklist;
}
