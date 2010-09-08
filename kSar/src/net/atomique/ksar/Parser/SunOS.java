/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar.Parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import net.atomique.ksar.OSParser;
import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;
import net.atomique.ksar.XML.GraphConfig;
import org.jfree.data.time.Second;

/**
 *
 * @author Max
 */
public class SunOS extends OSParser {


    public void parse_header(String s) {
        String [] columns = s.split("\\s+");
        setOstype(columns[0]);
        setHostname(columns[1]);
        setOSversion(columns[2]);
        setKernel(columns[3]);
        setCpuType(columns[4]);
        setDate(columns[5]);
    }

    @Override
    public int parse(String line, String[] columns) {
        int heure = 0;
        int minute = 0;
        int seconde = 0;


        if ("Average".equals(columns[0])) {
            under_average = true;
            return 0;
        }

        if (line.indexOf("unix restarts") >= 0 || line.indexOf(" unix restarted") >= 0) {
            return 0;
        }

        // match the System [C|c]onfiguration line on AIX
        if (line.indexOf("System Configuration") >= 0 || line.indexOf("System configuration") >= 0) {
            return 0;
        }

        if (line.indexOf("State change") >= 0) {
            return 0;
        }


        try {
            parsedate = new SimpleDateFormat(timeFormat).parse(columns[0]);
            cal.setTime(parsedate);
            heure = cal.get(cal.HOUR_OF_DAY);
            minute = cal.get(cal.MINUTE);
            seconde = cal.get(cal.SECOND);
            now = new Second(seconde, minute, heure, day, month, year);
            if (startofstat == null) {
                startofstat = now;
                startofgraph =now;
            }
            if ( endofstat == null) {
                endofstat = now;
                endofgraph = now;
            }
            if (now.compareTo(endofstat) > 0) {
                endofstat = now;
                endofgraph = now;
            }
            firstdatacolumn = 1;
        } catch (ParseException ex) {
            if (! "DEVICE".equals(currentStat)) {
                System.out.println("unable to parse time " + columns[0]);
                return -1;
            }
            firstdatacolumn = 0;
        }


        /** XML COLUMN PARSER **/
        String checkStat = myosconfig.getStat(columns, firstdatacolumn);

        if (checkStat != null) {
            Object obj = ListofGraph.get(checkStat);
            if (obj == null) {
                GraphConfig mygraphinfo = myosconfig.getGraphConfig(checkStat);
                if (mygraphinfo != null) {
                    if ("unique".equals(mygraphinfo.getType())) {
                        obj = new Graph(mysar, mygraphinfo, mygraphinfo.getTitle(), line, firstdatacolumn, mysar.graphtree);
                        ListofGraph.put(checkStat, obj);
                        currentStat = checkStat;
                        return 0;
                    }
                    if ("multiple".equals(mygraphinfo.getType())) {
                        obj = new List(mysar, mygraphinfo, mygraphinfo.getTitle(), line, firstdatacolumn);
                        ListofGraph.put(checkStat, obj);
                        currentStat = checkStat;
                        return 0;
                    }
                } else {
                    // no graph associate
                    currentStat = checkStat;
                    return 0;
                }
            } else {
                currentStat = checkStat;
                return 0;
            }
        }

        //System.out.println(currentStat + " " + line);



        if (lastStat != null) {
            if (!lastStat.equals(currentStat) ) {
                if (  GlobalOptions.isDodebug())  {
                System.out.println("Stat change from " + lastStat + " to " + currentStat);
                }
                lastStat = currentStat;
                under_average = false;
            }
        } else {
            lastStat = currentStat;
        }
        if ("IGNORE".equals(currentStat)) {
            return 1;
        }
        if ("NONE".equals(currentStat)) {
            return -1;
        }

        if (under_average) {
            return 0;
        }
        currentStatObj = ListofGraph.get(currentStat);
        if (currentStatObj == null) {
            return -1;
        } else {
            DateSamples.add(now);
            if (currentStatObj instanceof Graph) {
                Graph ag = (Graph) currentStatObj;
                return ag.parse_line(now, line);
            }
            if (currentStatObj instanceof List) {
                List ag = (List) currentStatObj;
                return ag.parse_line(now, line);
            }
        }
        return -1;
    }
    
    Second now = null;
    boolean under_average = false;
    

}
