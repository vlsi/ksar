/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.atomique.ksar.XML;

import net.atomique.ksar.UI.NaturalComparator;

import java.util.SortedMap;
import java.util.TreeMap;

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
        plotlist.put(s.getTitle(), s);
    }

    public SortedMap<String, PlotConfig> getPlotlist() {
        return plotlist;
    }

    public void addStack(StackConfig s) {
        stacklist.put(s.getTitle(), s);
    }

    public SortedMap<String, StackConfig> getStacklist() {
        return stacklist;
    }

    private String name =null;
    private String Title = null;
    private String type = null;
    private SortedMap<String,PlotConfig> plotlist = new TreeMap<>(NaturalComparator.NULLS_FIRST);
    private SortedMap<String,StackConfig> stacklist = new TreeMap<>(NaturalComparator.NULLS_FIRST);
}
