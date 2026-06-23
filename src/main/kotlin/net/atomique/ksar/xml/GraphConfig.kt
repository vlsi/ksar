/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

import java.util.LinkedHashMap;

public class GraphConfig {

  public GraphConfig(String name, String title, String type) {
    this.name = name;
    this.title = title;
    this.type = type;
    plotlist = new LinkedHashMap<>();
    stacklist = new LinkedHashMap<>();
  }

  public String getTitle() {
    return title;
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

  private String name;
  private String title;
  private String type;
  private LinkedHashMap<String, PlotStackConfig> plotlist;
  private LinkedHashMap<String, PlotStackConfig> stacklist;
}
