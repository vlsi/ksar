/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

public class ColumnConfig {

  private static final Logger log = LoggerFactory.getLogger(ColumnConfig.class);

  public ColumnConfig(String s) {
    data_title = s;
  }

  public void setData_color(String dataColorString) {

    String[] color_indices = dataColorString.split(",");

    if (color_indices.length == 3) {
      try {
        this.data_color = new Color(Integer.parseInt(color_indices[0]),
                Integer.parseInt(color_indices[1]),
                Integer.parseInt(color_indices[2]));
      } catch (IllegalArgumentException iae) {
        log.warn("Column color error for {} - <{}>", data_title, dataColorString, iae);
      }
    }  else {
      log.warn("Wrong Color definition for {} - <{}>",data_title, dataColorString);
    }
  }

  public Color getData_color() {
    return data_color;
  }

  public String getData_title() {
    return data_title;
  }

  public void setType(String s) {
    if ("gauge".equals(s)) {
      type = 1;
    }
    if ("counter".equals(s)) {
      type = 2;
    }
  }

  public int getType() {
    return type;
  }

  public boolean is_valid() {

    return data_color != null;
  }

  private int type = 0;
  private Color data_color = null;
  private String data_title;
}
