package net.atomique.ksar.Parser;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.atomique.ksar.Config;
import net.atomique.ksar.OSParser;
import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;
import net.atomique.ksar.UI.LinuxDateFormat;
import net.atomique.ksar.XML.GraphConfig;

public class Linux extends OSParser {

    private String LinuxDateFormat;
    private LocalTime prevParseTime;

    private final Set<String> IgnoreLinesBeginningWith = new HashSet<String>(Arrays.asList(
            new String[] {"Average:","##","Summary"}
    ));

    public void parse_header(String s) {

        LinuxDateFormat = Config.getLinuxDateFormat();
        String[] columns = s.split("\\s+");
        String tmpstr;
        setOstype(columns[0]);
        setKernel(columns[1]);
        tmpstr = columns[2];
        setHostname(tmpstr.substring(1, tmpstr.length() - 1));
        checkDateFormat();
        setDate(columns[3]);

    }

    private void checkDateFormat() {

        if ("Always ask".equals(LinuxDateFormat)) {
            askDateFormat("Provide date Format");
        }
        
        if ("MM/DD/YYYY 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "MM/dd/yy";
        } else if ("MM/DD/YY 12:59:59 AM|PM".equals(LinuxDateFormat)) {
            dateFormat = "MM/dd/yy";
            timeFormat = "hh:mm:ss a";
            timeColumn=2;
        } else if ("YYYY-MM-DD 12:59:59 AM|PM".equals(LinuxDateFormat)) {
            dateFormat = "yyyy-MM-dd";
            timeFormat = "hh:mm:ss a";
            timeColumn=2;
        } else if ("DD/MM/YYYY 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "dd/MM/yy";
        } else if ("YYYY-MM-DD 23:59:59".equals(LinuxDateFormat)) {
            dateFormat = "yyyy-MM-dd";
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

        if (IgnoreLinesBeginningWith.contains(columns[0])) {
            currentStat = "NONE";
            return 0;
        }

        if (line.contains("LINUX RESTART")) {
            return 0;
        }

        try {
            if ( timeColumn == 2 ) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat,Locale.US);
                parsetime = LocalTime.parse(columns[0]+" "+columns[1], formatter);
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                parsetime = LocalTime.parse(columns[0],formatter);
            }

            LocalDateTime nowStat;
            nowStat = LocalDateTime.of(parsedate, parsetime);

            if (startofgraph == null) {
                startofgraph = nowStat;
            }
            if (endofgraph == null) {
                endofgraph = nowStat;
            }
            if (nowStat.compareTo(endofgraph) > 0) {
                endofgraph = nowStat;
            }
            firstdatacolumn = timeColumn;
        } catch (DateTimeParseException ex) {
            System.out.println("unable to parse time " + columns[0]);
            return -1;
        }


        //00:20:01     CPU  i000/s  i001/s  i002/s  i008/s  i009/s  i010/s  i011/s  i012/s  i014/s
        if ("CPU".equals(columns[firstdatacolumn]) && line.matches(".*i([0-9]+)/s.*")) {
            currentStat = "IGNORE";
            return 1;
        }
        // XML COLUMN PARSER
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
