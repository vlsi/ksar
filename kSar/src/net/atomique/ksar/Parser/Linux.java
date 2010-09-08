/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar.Parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import net.atomique.ksar.Config;
import net.atomique.ksar.OSParser;
import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;
import net.atomique.ksar.UI.LinuxDateFormat;
import net.atomique.ksar.XML.GraphConfig;
import org.jfree.data.time.Second;

/**
 *
 * @author Max
 */
public class Linux extends OSParser {

    public void parse_header(String s) {
        boolean retdate = false;
        LinuxDateFormat = Config.getLinuxDateFormat();
        String[] columns = s.split("\\s+");
        String tmpstr;
        setOstype(columns[0]);
        setKernel(columns[1]);
        tmpstr = columns[2];
        setHostname(tmpstr.substring(1, tmpstr.length() - 1));
        checkDateFormat();
        retdate=setDate(columns[3]);
        
    }

    private void checkDateFormat() {

        if ("Always ask".equals(LinuxDateFormat)) {
            askDateFormat("Provide date Format");
        }
        
        if ("MM/DD/YYYY 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "MM/dd/yy";
        } else if ("MM/DD/YYYY 12:59:59 AM|PM".equals(LinuxDateFormat)) {
            dateFormat = "MM/dd/yy";
            timeFormat = "hh:mm:ss a";
        } else if ("DD/MM/YYYY 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "dd/MM/yy";
        } else if ("YYYY-MM-DD 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "yy-MM-dd";
        }  
    }

    private void askDateFormat(String s) {
        if ( GlobalOptions.hasUI() ) {
            LinuxDateFormat tmp = new LinuxDateFormat(GlobalOptions.getUI(),true);
            tmp.setTitle(s);
            if ( tmp.isOk()) {
                LinuxDateFormat=tmp.getDateFormat();
                if ( tmp.hasToRemenber() ) {
                    Config.setLinuxDateFormat(tmp.getDateFormat());
                    Config.save();
                }
            }
        }
    }
    
    @Override
    public int parse(String line, String[] columns) {
        int heure = 0;
        int minute = 0;
        int seconde = 0;
        Second now = null;

        if ("Average:".equals(columns[0])) {
            currentStat = "NONE";
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
            if ( timeColumn ==2) {
                parsedate = new SimpleDateFormat(timeFormat).parse(columns[0]+" "+columns[1]);
            } else {
                parsedate = new SimpleDateFormat(timeFormat).parse(columns[0]);
            }
            cal.setTime(parsedate);
            heure = cal.get(cal.HOUR_OF_DAY);
            minute = cal.get(cal.MINUTE);
            seconde = cal.get(cal.SECOND);
            now = new Second(seconde, minute, heure, day, month, year);
            if (startofstat == null) {
                startofstat = now;
                startofgraph = now;
            }
            if (endofstat == null) {
                endofstat = now;
                endofgraph = now;
            }
            if (now.compareTo(endofstat) > 0) {
                endofstat = now;
                endofgraph = now;
            }
            firstdatacolumn = timeColumn;
        } catch (ParseException ex) {
            System.out.println("unable to parse time " + columns[0]);
            return -1;
        }


        //00:20:01     CPU  i000/s  i001/s  i002/s  i008/s  i009/s  i010/s  i011/s  i012/s  i014/s
        if ("CPU".equals(columns[firstdatacolumn]) && line.matches(".*i([0-9]+)/s.*")) {
            currentStat = "IGNORE";
            return 1;
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

        //System.out.println( currentStat +" " + line);



        if (lastStat != null) {
            if (!lastStat.equals(currentStat) ) {
                if (  GlobalOptions.isDodebug())  {
                System.out.println("Stat change from " + lastStat + " to " + currentStat);
                }
                lastStat = currentStat;
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


    private String LinuxDateFormat;
    
}
