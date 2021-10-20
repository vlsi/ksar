/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.parser

import net.atomique.ksar.kSar
import net.atomique.ksar.OSParser
import net.atomique.ksar.graph.Graph
import net.atomique.ksar.graph.List
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory

class HPUX(hissar: kSar, header: String) : OSParser(hissar, header) {
    var under_average = false

    companion object {
        private val log = LoggerFactory.getLogger(HPUX::class.java)
    }

    override fun parse_header(s: String) {
        val columns = s.split(Regex("\\s+"))
        ostype = columns[0]
        Hostname = columns[1]
        OSversion = columns[2]
        Kernel = columns[3]
        CpuType = columns[4]
        setDate(columns[5])
    }

    override fun parse(line: String, columns: Array<String>): Int {
        if ("Average" == columns[0]) {
            under_average = true
            return 0
        }
        if (line.indexOf("unix restarts") >= 0 || line.indexOf(" unix restarted") >= 0) {
            return 0
        }

        // match the System [C|c]onfiguration line on AIX
        if (line.indexOf("System Configuration") >= 0 || line.indexOf("System configuration") >= 0) {
            return 0
        }
        if (line.indexOf("State change") >= 0) {
            return 0
        }
        try {
            timeFormat = "HH:mm:ss"
            val formatter = DateTimeFormatter.ofPattern(timeFormat)
            parsetime = LocalTime.parse(columns[0], formatter)
            val nowStat = LocalDateTime.of(parsedate, parsetime)
            setStartAndEndOfGraph(nowStat)
            firstdatacolumn = 1
        } catch (ex: DateTimeParseException) {
            if ("DEVICE" != currentStat && "CPU" != currentStat) {
                log.error("unable to parse time {}", columns[0], ex)
                return -1
            }
            firstdatacolumn = 0
        }
        /** XML COLUMN PARSER  */
        val checkStat = myosconfig.getStat(columns, firstdatacolumn)
        if (checkStat != null) {
            var obj = ListofGraph[checkStat]
            if (obj == null) {
                val mygraphinfo = myosconfig.getGraphConfig(checkStat)
                if (mygraphinfo != null) {
                    if ("unique" == mygraphinfo.type) {
                        obj = Graph(
                            mysar, mygraphinfo, mygraphinfo.title, line, firstdatacolumn,
                            mysar.graphtree
                        )
                        ListofGraph[checkStat] = obj
                        currentStat = checkStat
                        return 0
                    }
                    if ("multiple" == mygraphinfo.type) {
                        obj = List(mysar, mygraphinfo, mygraphinfo.title, line, firstdatacolumn)
                        ListofGraph[checkStat] = obj
                        currentStat = checkStat
                        return 0
                    }
                } else {
                    // no graph associate
                    currentStat = checkStat
                    return 0
                }
            } else {
                currentStat = checkStat
                return 0
            }
        }

        // log.trace("{} {}", currentStat, line);
        if (lastStat != null) {
            if (lastStat != currentStat) {
                log.debug("Stat change from {} to {}", lastStat, currentStat)
                lastStat = currentStat
                under_average = false
            }
        } else {
            lastStat = currentStat
        }
        if ("IGNORE" == currentStat) {
            return 1
        }
        if ("NONE" == currentStat) {
            return -1
        }
        if (under_average) {
            return 0
        }
        currentStatObj = ListofGraph[currentStat]
        if (currentStatObj == null) {
            return -1
        } else {
            val nowStat = LocalDateTime.of(parsedate, parsetime)
            dateSamples.add(nowStat)
            if (currentStatObj is Graph) {
                val ag = currentStatObj as Graph
                return ag.parse_line(nowStat, line)
            }
            if (currentStatObj is List) {
                val ag = currentStatObj as List
                return ag.parse_line(nowStat, line)
            }
        }
        return -1
    }
}
