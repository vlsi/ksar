/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

public class HostInfo {

  public HostInfo(String s) {
    this.setHostname(s);
  }

  public Integer getMemBlockSize() {
    return MemBlockSize;
  }

  public void setMemBlockSize(Integer MemBlockSize) {
    this.MemBlockSize = MemBlockSize;
  }

  public void setMemBlockSize(String MemBlockSizestr) {
    try {
      this.MemBlockSize = Integer.parseInt(MemBlockSizestr);
    } catch (NumberFormatException nfe) {
    }
  }

  public String getAlias() {
    return aka_hostname;
  }

  public void setAlias(String aka_hostname) {
    this.aka_hostname = aka_hostname;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getHostname() {
    return sar_hostname;
  }

  public void setHostname(String sar_hostname) {
    this.sar_hostname = sar_hostname;
  }

  public String save() {
    StringBuilder tmp = new StringBuilder();
    tmp.append("\t\t<host name=\"" + sar_hostname + "\">\n");
    tmp.append("\t\t\t<alias>" + aka_hostname + "</alias>\n");
    tmp.append("\t\t\t<description>" + description + "</description>\n");
    tmp.append("\t\t\t<memblocksize>" + MemBlockSize + "</memblocksize>\n");
    tmp.append("\t\t</host>\n");
    return tmp.toString();
  }


  private String sar_hostname = null;
  private String aka_hostname = null;
  private String description = null;
  private Integer MemBlockSize = 1;

}
