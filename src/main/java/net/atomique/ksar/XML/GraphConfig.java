package net.atomique.ksar.XML;

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
    
    public void addPlot(PlotConfig s) {
        plotlist.put(s.getTitle(), s);
    }

    public LinkedHashMap<String, PlotConfig> getPlotlist() {
        return plotlist;
    }

    public void addStack(StackConfig s) {
        stacklist.put(s.getTitle(), s);
    }

    public LinkedHashMap<String, StackConfig> getStacklist() {
        return stacklist;
    }

    private String name =null;
    private String Title = null;
    private String type = null;
    private LinkedHashMap<String,PlotConfig> plotlist;
    private LinkedHashMap<String,StackConfig> stacklist;
}
