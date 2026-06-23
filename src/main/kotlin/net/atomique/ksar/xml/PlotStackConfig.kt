/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

import net.atomique.ksar.graph.IEEE1541Number;
import net.atomique.ksar.graph.ISNumber;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;

public class PlotStackConfig {
  private static final Logger log = LoggerFactory.getLogger(PlotStackConfig.class);


  public PlotStackConfig(String title) {
    this.title = title;
  }

  public String[] getHeader() {
    return Header;
  }

  public String getTitle() {
    return title;
  }

  public void setHeaderStr(String s) {
    this.Header = s.split("\\s+");
    HeaderStr = s;
  }

  public String getHeaderStr() {
    return HeaderStr;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public void setSize(String size) {
    this.size = Integer.valueOf(size);
  }

  public NumberAxis getAxis() {
    NumberAxis tmp = new NumberAxis(title);

    if (base == 1024) {

      NumberFormat decimalformat1 = new IEEE1541Number((int) factor);
      tmp.setNumberFormatOverride(decimalformat1);

    } else if (base == 1000) {

      NumberFormat decimalformat1 = new ISNumber((int) factor);
      tmp.setNumberFormatOverride(decimalformat1);

    } else if (base != 0) {
      log.error("base value is not handled");
    }

    if (range != null) {
      tmp.setRange(range);
    }
    return tmp;
  }

  public void setBase(String s) {
    if (s == null) {
      return;
    }
    base = Integer.parseUnsignedInt(s);
  }

  public void setFactor(String s) {
    factor = Double.parseDouble(s);
  }

  public void setRange(String s) {
    String[] t = s.split(",");
    if (t.length == 2) {
      double min = Double.parseDouble(t[0]);
      double max = Double.parseDouble(t[1]);
      range = new Range(min, max);
    }
  }

  private double factor;
  private int base = 0;
  private Range range = null;
  private int size = 1;
  private String title;
  private String[] Header = null;
  private String HeaderStr = null;
}
