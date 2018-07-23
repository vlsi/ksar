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
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AllParser {

  private static final Logger log = LoggerFactory.getLogger(AllParser.class);

  private static final List<DateTimeFormatter> DATE_FORMATS = Stream.of(
      "MM dd, yy",
      "MM-dd-yy",
      "MM/dd/yy",
      "MM-dd-yyyy",
      "MM/dd/yyyy",
      "dd-MM-yy",
      "dd.MM.yy",
      "dd/MM/yy",
      "dd.MM.yy.",
      "dd-MM-yyyy",
      "dd.MM.yyyy",
      "dd/MM/yyyy",
      "dd.MM.yyyy.",
      "yy. MM. dd",
      "yy-MM-dd",
      "yy.MM.dd",
      "yy/MM/dd",
      "yy年MM月dd日",
      "yy.dd.MM",
      "yyyy. MM. dd",
      "yyyy-MM-dd",
      "yyyy.MM.dd",
      "yyyy/MM/dd",
      "yyyy.MM.dd.",
      "yyyy年MM月dd日",
      "yyyy.dd.MM",
      "yyyyMMdd",
      "dd MMM yyyy",
      "dd MMMM yyyy",
      "MMM dd yyyy",
      "MMMM dd yyyy"
  ).map(p -> DateTimeFormatter.ofPattern(p, Locale.US)).collect(Collectors.toList());

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

  /**
   * Set {@link #startOfGraph} and {@link #endOfGraph} to the given value if none are available yet
   * or update either of both, depending on if the given value is earlier/later than the formerly
   * stored corresponding one.
   *
   * @param nowStat Date/time of the currently parsed line.
   */
  protected void setStartAndEndOfGraph(LocalDateTime nowStat) {
    if (startOfGraph == null) {
      startOfGraph = nowStat;
    }
    if (endOfGraph == null) {
      endOfGraph = nowStat;
    }

    if (nowStat.compareTo(startOfGraph) < 0) {
      startOfGraph = nowStat;
    }
    if (nowStat.compareTo(endOfGraph) > 0) {
      endOfGraph = nowStat;
    }
  }

  public LocalDateTime getStartOfGraph() {
    return startOfGraph;
  }

  public LocalDateTime getEndOfGraph() {
    return endOfGraph;
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
      DateTimeFormatter formatter = getDateFormatter(s);
      currentDate = LocalDate.parse(s, formatter);

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

  private DateTimeFormatter getDateFormatter(String s) {
    if (dateFormatter != null) {
      return dateFormatter;
    }
    DateTimeFormatter format = null;
    if ("Automatic Detection".equals(dateFormat)) {
      format = determineDateFormat(s);
    } else {
      format = DateTimeFormatter.ofPattern(dateFormat);
    }
    dateFormatter = format;
    return dateFormatter;
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

  public static DateTimeFormatter determineDateFormat(String dateString) {
    DateTimeFormatter best = null;
    LocalDate bestDate = null;
    for (DateTimeFormatter format : DATE_FORMATS) {
      try {
        LocalDate nextDate = LocalDate.parse(dateString, format);
        if (bestDate == null || nextDate.compareTo(bestDate) >= 0) {
          bestDate = nextDate;
          best = format;
        }
      } catch (DateTimeParseException e) {
        /* ignore */
      }
    }
    return best;
  }

  protected String sarStartDate = null;
  protected String sarEndDate = null;

  private LocalDateTime startOfGraph = null;
  private LocalDateTime endOfGraph = null;

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

  private DateTimeFormatter dateFormatter;
}
