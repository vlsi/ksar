package net.atomique.ksar.Parser;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import net.atomique.ksar.OSParser;
import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;
import net.atomique.ksar.UI.HostInfoView;
import net.atomique.ksar.XML.HostInfo;
import net.atomique.ksar.XML.GraphConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SunOS extends OSParser {

    private static final Logger log = LoggerFactory.getLogger(SunOS.class);

    boolean under_average = false;

    public void parse_header(String s) {

        String[] columns = s.split("\\s+");

        setOstype(columns[0]);
        setHostname(columns[1]);
        setOSversion(columns[2]);
        setKernel(columns[3]);
        setCpuType(columns[4]);
        setDate(columns[5]);
        
        if (GlobalOptions.hasUI()) {
            HostInfo tmphostinfo = GlobalOptions.getHostInfo(this.gethostName());
            if (tmphostinfo == null) {
                tmphostinfo = new HostInfo(this.gethostName());
            }
            HostInfoView tmpview = new HostInfoView(GlobalOptions.getUI(), tmphostinfo);
            tmpview.setVisible(true);
           
        }

    }

    @Override
    public int parse(String line, String[] columns) {

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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
            parsetime = LocalTime.parse(columns[0],formatter);

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
            firstdatacolumn = 1;
        } catch (DateTimeParseException ex) {
            if (!"DEVICE".equals(currentStat)) {
                log.error("unable to parse time {}", columns[0], ex);
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

        //log.trace("{} {}", currentStat, line);



        if (lastStat != null) {
            if (!lastStat.equals(currentStat)) {
                if (GlobalOptions.isDodebug()) {
                    log.debug("Stat change from {} to {}", lastStat, currentStat);
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
