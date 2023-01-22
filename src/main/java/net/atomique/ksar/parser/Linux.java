/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.parser;

import net.atomique.ksar.Config;
import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.OSParser;
import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.graph.List;
import net.atomique.ksar.ui.LinuxDateFormat;
import net.atomique.ksar.xml.GraphConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

public class Linux extends OSParser {

  private static final Logger log = LoggerFactory.getLogger(Linux.class);
  private String LinuxDateFormat;
  private DateTimeFormatter formatter;

  // https://translationproject.org/domain/sysstat.html
  // https://github.com/sysstat/sysstat/tree/master/nls
  // look for - msgid "Average:" && msgid "Summary:"
  private final HashSet<String> IgnoreLinesBeginningWith = new HashSet<>(Arrays.asList(
      "##",
      "Average:", "Summary:", "Summary",              // en.po
      "Moyenne :", "Moyenne?:", "Résumé:",        // fr.po (ANSI,UTF-8)
      "Durchschn.:", "Zusammenfassung:",              // de.po
      "Media:", "Resumen:"                            // es.po
  ));

  public void parse_header(String s) {
    //Linux 3.10.0-1160.53.1.el7.x86_64 (hostname)  01/03/23  _x86_64_  (8 CPU)

    log.debug("Header Line : {}", s);
    String[] columns = s.split("\\s+", 5);

    setOstype(columns[0]);
    setKernel(columns[1]);

    // remove surrounding brackets (hostname)
    setHostname(columns[2].substring(1, columns[2].length() - 1));

    checkDateFormat();
    setDate(columns[3]);
  }

  private void checkDateFormat() {

    LinuxDateFormat = Config.getLinuxDateFormat();
    if ("Always ask".equals(LinuxDateFormat)) {
      askDateFormat();
    }

    if ("Automatic Detection".equals(LinuxDateFormat)) {
      dateFormat = "Automatic Detection";
      timeColumn = 0;
    } else {

      // day and year format specifiers must be lowercase, month uppercase
      String[] parts = LinuxDateFormat.split(" ", 3);

      dateFormat = parts[0];
      dateFormat = dateFormat.replaceAll("D{2}", "dd");
      dateFormat = dateFormat.replaceAll("Y{2}", "yy");

      // 12hour
      if (parts.length == 3 && parts[2].contains("AM|PM")) {
        timeFormat = "hh:mm:ss a";
        timeColumn = 2;
      }
    }
    log.debug("Date Format: {}, Time Format: {}", dateFormat, timeFormat);
  }

  private void askDateFormat() {

    log.trace("askDateFormat - provide date format");
    if (GlobalOptions.hasUI()) {
      LinuxDateFormat tmp = new LinuxDateFormat(GlobalOptions.getUI(), true);
      tmp.setTitle("Provide date format");
      if (tmp.isOk()) {
        LinuxDateFormat = tmp.getDateFormat();
        if (tmp.hasToRemenber()) {
          Config.setLinuxDateFormat(tmp.getDateFormat());
          Config.save();
        }
      }
    }
  }

  @Override
  public int parse(String line, String[] columns) {

    if (IgnoreLinesBeginningWith.contains(columns[0])) {
      currentStat = "NONE";
      return 1;
    }

    if (line.contains("LINUX RESTART")) {
      return 1;
    }

    try {
      if (timeColumn == 0) {
        if ((columns[0] + " " + columns[1]).matches("^\\d\\d:\\d\\d:\\d\\d [AP]M$")) {
          timeFormat = "hh:mm:ss a";
          timeColumn = 2;
        } else {
          timeColumn = 1;
        }
      }

      if (formatter == null) {
        if (timeColumn == 2) {
          formatter = DateTimeFormatter.ofPattern(timeFormat, Locale.US);
        } else {
          formatter = DateTimeFormatter.ofPattern(timeFormat);
        }
        log.debug("Time formatter: {}",formatter);
      }

      if (timeColumn == 2) {
        parsetime = LocalTime.parse(columns[0] + " " + columns[1], formatter);
      } else {
        parsetime = LocalTime.parse(columns[0], formatter);
      }

      LocalDateTime nowStat;
      if (parsedate != null && parsetime != null) {
        nowStat = LocalDateTime.of(parsedate, parsetime);
      } else {
        throw new IllegalArgumentException("date/time is missing");
      }

      this.setStartAndEndOfGraph(nowStat);
      firstdatacolumn = timeColumn;
    } catch (DateTimeParseException | IllegalArgumentException ex) {
      log.error("unable to parse time {}", columns[0], ex);
      return -1;
    }

    // XML COLUMN PARSER
    String checkStat = myosconfig.getStat(columns, firstdatacolumn);
    if (checkStat != null) {
      Object obj = ListofGraph.get(checkStat);
      if (obj == null) {
        GraphConfig mygraphinfo = myosconfig.getGraphConfig(checkStat);
        if (mygraphinfo != null) {
          if ("unique".equals(mygraphinfo.getType())) {
            obj = new Graph(mysar, mygraphinfo, mygraphinfo.getTitle(), line, firstdatacolumn,
                mysar.graphtree);

            ListofGraph.put(checkStat, obj);
            currentStat = checkStat;
            return 2;
          }
          if ("multiple".equals(mygraphinfo.getType())) {
            obj = new List(mysar, mygraphinfo, mygraphinfo.getTitle(), line, firstdatacolumn);

            ListofGraph.put(checkStat, obj);
            currentStat = checkStat;
            return 2;
          }
        } else {
          // no graph associate
          currentStat = checkStat;
          return 3;
        }
      } else {
        currentStat = checkStat;
        return 2;
      }
    }

    if (lastStat != null) {
      if (!lastStat.equals(currentStat)) {
        log.debug("Stat change from {} to {}", lastStat, currentStat);
        lastStat = currentStat;
      }
    } else {
      lastStat = currentStat;
    }

    if ("IGNORE".equals(currentStat)) {
      return 0;
    }
    if ("NONE".equals(currentStat)) {
      return -1;
    }

    currentStatObj = ListofGraph.get(currentStat);
    if (currentStatObj == null) {
      return -1;
    } else {

      LocalDateTime nowStat = LocalDateTime.of(parsedate, parsetime);

      DateSamples.add(nowStat);

      if (currentStatObj instanceof Graph) {
        Graph ag = (Graph) currentStatObj;
        return ag.parse_line(nowStat, line);
      }
      if (currentStatObj instanceof List) {
        List ag = (List) currentStatObj;
        return ag.parse_line(nowStat, line);
      }
    }
    return -1;
  }

}
