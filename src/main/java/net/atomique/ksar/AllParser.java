/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.xml.OSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public abstract class AllParser {

  private static final Logger log = LoggerFactory.getLogger(AllParser.class);
  
  private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
      put("^\\d{8}$", "yyyyMMdd");
      put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
      put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
      put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
      put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
      put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
      put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
      put("^\\d{1,2}-\\d{1,2}-\\d{2}$", "dd-MM-yy");
      put("^\\d{1,2}/\\d{1,2}/\\d{2}$", "MM/dd/yy");
  }};
  
  public AllParser() {

  }

  public void init(kSar hissar, String header) {
    String[] s = header.split("\\s+");
    mysar = hissar;
    ParserName = s[0];
    parse_header(header);
  }

  public AllParser(kSar hissar, String header) {
    init(hissar, header);
  }

  public int parse(String line, String[] columns) {
    log.error("not implemented");
    return -1;
  }

  public LocalDateTime get_startofgraph() {
    return startofgraph;
  }

  public LocalDateTime get_endofgraph() {
    return endofgraph;
  }

  public String getParserName() {
    return ParserName;
  }

  public boolean setDate(String s) {
    LocalDate currentDate;
    LocalDate startDate;
    LocalDate endDate;

    if (sarStartDate == null) {
      sarStartDate = s;
    }
    if (sarEndDate == null) {
      sarEndDate = s;
    }

    try {
    	DateTimeFormatter formatter;
    	if(dateFormat.equals("Autodetect")){
        	formatter = DateTimeFormatter.ofPattern(determineDateFormat(s));
        	currentDate = LocalDate.parse(s, formatter);
    	}else{
	      formatter = DateTimeFormatter.ofPattern(dateFormat);
	      currentDate = LocalDate.parse(s, formatter);
	    	}

      parsedate = currentDate;

      startDate = LocalDate.parse(sarStartDate, formatter);
      endDate = LocalDate.parse(sarEndDate, formatter);

    } catch (DateTimeParseException ex) {
      log.error("unable to parse date {}", s, ex);
      return false;
    }

    if (currentDate.compareTo(startDate) < 0) {
      sarStartDate = s;
    }
    if (currentDate.compareTo(endDate) > 0) {
      sarEndDate = s;
    }
    return true;
  }

  public String getDate() {
    if (sarStartDate.equals(sarEndDate)) {
      return sarStartDate;
    } else {
      return sarStartDate + " to " + sarEndDate;
    }
  }

  public TreeSet<LocalDateTime> getDateSamples() {
    return DateSamples;
  }

  public String getCurrentStat() {
    return currentStat;
  }
  
  public static String determineDateFormat(String dateString) {
      for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
          if (dateString.toLowerCase().matches(regexp)) {
              return DATE_FORMAT_REGEXPS.get(regexp);
          }
      }
      return null; // Unknown format.
  }


  protected String sarStartDate = null;
  protected String sarEndDate = null;

  protected LocalDateTime startofgraph = null;
  protected LocalDateTime endofgraph = null;
  protected TreeSet<LocalDateTime> DateSamples = new TreeSet<LocalDateTime>();
  protected int firstdatacolumn = 0;

  abstract public String getInfo();

  abstract public void parse_header(String s);

  abstract public void updateUITitle();

  protected kSar mysar = null;
  protected OSConfig myosconfig = null;
  protected String ParserName = null;

  protected LocalTime parsetime = null;
  protected LocalDate parsedate = null;

  protected String currentStat = "NONE";
  protected String dateFormat = "MM/dd/yy";
  protected String timeFormat = "HH:mm:ss";
  protected int timeColumn = 1;
}
