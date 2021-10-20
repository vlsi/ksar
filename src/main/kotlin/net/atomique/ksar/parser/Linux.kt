/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.parser

import net.atomique.ksar.Config
import net.atomique.ksar.GlobalOptions
import net.atomique.ksar.OSParser
import net.atomique.ksar.graph.Graph
import net.atomique.ksar.graph.List
import net.atomique.ksar.kSar
import net.atomique.ksar.ui.LinuxDateFormat
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

class Linux(hissar: kSar, header: String) : OSParser(hissar, header) {
    private lateinit var LinuxDateFormat: String
    private var formatter: DateTimeFormatter? = null
    private val IgnoreLinesBeginningWith = setOf(
        "Average:", "##", "Summary", "Summary:"
    )

    companion object {
        private val log = LoggerFactory.getLogger(Linux::class.java)
    }

    override fun parse_header(s: String) {
        log.debug("Header Line : {}", s)
        val columns = s.split(Regex("\\s+"), limit = 5)
        ostype = columns[0]
        Kernel = columns[1]
        val tmpstr = columns[2]
        Hostname = tmpstr.substring(1, tmpstr.length - 1)
        checkDateFormat()
        setDate(columns[3])
    }

    private fun checkDateFormat() {
        LinuxDateFormat = Config.linuxDateFormat
        if ("Always ask" == LinuxDateFormat) {
            askDateFormat()
        }
        if ("Automatic Detection" == LinuxDateFormat) {
            dateFormat = "Automatic Detection"
            timeColumn = 0
        } else {
            // day and year format specifiers must be lower case, month upper case
            val parts = LinuxDateFormat.split(" ", limit = 3)
            dateFormat = parts[0]
            dateFormat = dateFormat.replace("D{2}".toRegex(), "dd")
            dateFormat = dateFormat.replace("Y{2}".toRegex(), "yy")

            // 12hour
            if (parts.size == 3 && parts[2].contains("AM|PM")) {
                timeFormat = "hh:mm:ss a"
                timeColumn = 2
            }
        }
        log.debug("Date Format: {}, Time Format: {}", dateFormat, timeFormat)
    }

    private fun askDateFormat() {
        log.trace("askDateFormat - provide date format")
        if (GlobalOptions.hasUI()) {
            val tmp = LinuxDateFormat(GlobalOptions.uI, true)
            tmp.title = "Provide date format"
            if (tmp.isOk) {
                LinuxDateFormat = tmp.dateFormat
                if (tmp.hasToRemenber()) {
                    Config.linuxDateFormat = tmp.dateFormat
                    Config.save()
                }
            }
        }
    }

    override fun parse(line: String, columns: Array<String>): Int {
        if (IgnoreLinesBeginningWith.contains(columns[0])) {
            currentStat = "NONE"
            return 0
        }
        if (line.contains("LINUX RESTART")) {
            log.debug("{}", line)
            return 0
        }
        try {
            if (timeColumn == 0) {
                if ((columns[0] + " " + columns[1]).matches(Regex("^\\d\\d:\\d\\d:\\d\\d [AP]M$"))) {
                    timeFormat = "hh:mm:ss a"
                    timeColumn = 2
                } else {
                    timeColumn = 1
                }
            }
            if (formatter == null) {
                formatter = if (timeColumn == 2) {
                    DateTimeFormatter.ofPattern(timeFormat, Locale.US)
                } else {
                    DateTimeFormatter.ofPattern(timeFormat)
                }
                log.debug("Time formatter: {}", formatter)
            }
            parsetime = if (timeColumn == 2) {
                LocalTime.parse(columns[0] + " " + columns[1], formatter)
            } else {
                LocalTime.parse(columns[0], formatter)
            }
            val nowStat = if (parsedate != null && parsetime != null) {
                LocalDateTime.of(parsedate, parsetime)
            } else {
                throw IllegalArgumentException("date/time is missing")
            }
            setStartAndEndOfGraph(nowStat)
            firstdatacolumn = timeColumn
        } catch (ex: DateTimeParseException) {
            log.error("unable to parse time {}", columns[0], ex)
            return -1
        } catch (ex: IllegalArgumentException) {
            log.error("unable to parse time {}", columns[0], ex)
            return -1
        }

        // 00:20:01     CPU  i000/s  i001/s  i002/s  i008/s  i009/s  i010/s  i011/s  i012/s  i014/s
        if ("CPU" == columns[firstdatacolumn] && line.matches(Regex(".*i([0-9]+)/s.*"))) {
            return 1
        }
        // XML COLUMN PARSER
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
        if (lastStat != null) {
            if (lastStat != currentStat) {
                log.debug("Stat change from {} to {}", lastStat, currentStat)
                lastStat = currentStat
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
