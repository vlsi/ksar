/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.xml.OSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

public abstract class OSParser extends AllParser {

  private static final Logger log = LoggerFactory.getLogger(OSParser.class);

  public OSParser() {

  }

  public void init(kSar hissar, String header) {
    String[] s = header.split("\\s+");
    mysar = hissar;
    ParserName = s[0];
    myosconfig = GlobalOptions.getOSinfo(s[0]);
    parse_header(header);
  }

  public OSParser(kSar hissar, String header) {
    init(hissar, header);
  }


  public OSConfig get_OSConfig() {
    return myosconfig;
  }


  public void setHostname(String s) {
    Hostname = s;
  }

  public void setOSversion(String s) {
    OSversion = s;
  }

  public void setKernel(String s) {
    Kernel = s;
  }

  public void setCpuType(String s) {
    CpuType = s;
  }


  public void setMacAddress(String s) {
    MacAddress = s;
  }

  public void setMemory(String s) {
    Memory = s;
  }

  public void setNBDisk(String s) {
    NBDisk = s;
  }

  public void setNBCpu(String s) {
    NBCpu = s;
  }

  public void setENT(String s) {
    ENT = s;
  }


  public String getInfo() {
    StringBuilder tmpstr = new StringBuilder();
    tmpstr.append("OS Type: ").append(ostype);
    if (OSversion != null) {
      tmpstr.append("OS Version: ").append(OSversion).append("\n");
    }
    if (Kernel != null) {
      tmpstr.append("Kernel Release: ").append(Kernel).append("\n");
    }
    if (CpuType != null) {
      tmpstr.append("CPU Type: ").append(CpuType).append("\n");
    }
    if (Hostname != null) {
      tmpstr.append("Hostname: ").append(Hostname).append("\n");
    }
    if (MacAddress != null) {
      tmpstr.append("Mac Address: ").append(MacAddress).append("\n");
    }
    if (Memory != null) {
      tmpstr.append("Memory: ").append(Memory).append("\n");
    }
    if (NBDisk != null) {
      tmpstr.append("Number of disks: ").append(NBDisk).append("\n");
    }
    if (NBCpu != null) {
      tmpstr.append("Number of CPU: ").append(NBCpu).append("\n");
    }
    if (ENT != null) {
      tmpstr.append("Ent: ").append(ENT).append("\n");
    }
    if (sarStartDate != null) {
      tmpstr.append("Start of SAR: ").append(sarStartDate).append("\n");
    }
    if (sarEndDate != null) {
      tmpstr.append("End of SAR: ").append(sarEndDate).append("\n");
    }

    tmpstr.append("\n");

    return tmpstr.toString();
  }


  public String gethostName() {
    return Hostname;
  }

  public String getOstype() {
    return ostype;
  }

  public void setOstype(String ostype) {
    this.ostype = ostype;
  }

  final public void updateUITitle() {
    if (mysar.getDataView() != null) {

      String asFormattedDateTimeStart = null;
      String asFormattedDateTimeEnd = null;

      try {

        //Locale test = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
        DateTimeFormatter formatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT); //.withLocale(test);

        if (startofgraph != null) {
          asFormattedDateTimeStart = startofgraph.format(formatter);
        }
        if (endofgraph != null) {
          //asFormattedDateTimeEnd = endofgraph.format(DateTimeFormatter.ISO_DATE_TIME);
          asFormattedDateTimeEnd = endofgraph.format(formatter);
        }

      } catch (DateTimeException ex) {
        log.error("unable to format time", ex);
      }

      if (asFormattedDateTimeStart != null && asFormattedDateTimeEnd != null) {
        mysar.getDataView()
            .setTitle(String.format("%s from %s to %s", Hostname, asFormattedDateTimeStart,
                asFormattedDateTimeEnd));
      }
    }
  }

  protected Map<String, Object> ListofGraph = new HashMap<String, Object>();

  protected String lastStat = null;
  protected Object currentStatObj = null;

  protected String ostype = null;
  protected String Hostname = null;
  protected String OSversion = null;
  protected String Kernel = null;
  protected String CpuType = null;
  protected String MacAddress = null;
  protected String Memory = null;
  protected String NBDisk = null;
  protected String NBCpu = null;
  protected String ENT = null;

}
