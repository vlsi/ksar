/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.xml.OSConfig
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

abstract class AllParser(protected val mysar: kSar, header: String) {
    protected var sarStartDate: String? = null
    protected var sarEndDate: String? = null
    var startOfGraph: LocalDateTime? = null
        private set
    var endOfGraph: LocalDateTime? = null
        private set
    var dateSamples = TreeSet<LocalDateTime>()
    protected var firstdatacolumn = 0
    abstract val info: String
    protected lateinit var myosconfig: OSConfig
    lateinit var parserName: String
        protected set
    protected var parsetime: LocalTime? = null
    protected var parsedate: LocalDate? = null
    var currentStat = "NONE"
        protected set
    protected var dateFormat = "MM/dd/yy"
    protected var timeFormat = "HH:mm:ss"
    protected var timeColumn = 1

    companion object {
        private val log = LoggerFactory.getLogger(AllParser::class.java)
        private val DATE_FORMAT_REGEXPS: Map<String, String> = mapOf(
            "^\\d{8}$" to "yyyyMMdd",
            "^\\d{1,2}-\\d{1,2}-\\d{4}$" to "dd-MM-yyyy",
            "^\\d{4}-\\d{1,2}-\\d{1,2}$" to "yyyy-MM-dd",
            "^\\d{1,2}/\\d{1,2}/\\d{4}$" to "MM/dd/yyyy",
            "^\\d{4}/\\d{1,2}/\\d{1,2}$" to "yyyy/MM/dd",
            "^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$" to "dd MMM yyyy",
            "^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$" to "dd MMMM yyyy",
            "^\\d{1,2}-\\d{1,2}-\\d{2}$" to "dd-MM-yy",
            "^\\d{1,2}/\\d{1,2}/\\d{2}$" to "MM/dd/yy"
        )

        fun determineDateFormat(dateString: String): String? {
            for (regexp in DATE_FORMAT_REGEXPS.keys) {
                if (dateString.lowercase(Locale.getDefault()).matches(Regex(regexp))) {
                    return DATE_FORMAT_REGEXPS[regexp]
                }
            }
            return null // Unknown format.
        }
    }

    init {
        init(header)
    }

    open fun init(header: String) {
        log.debug("Initialize Parser: {}", this.javaClass.name)
        parserName = header.split(Regex("\\s+"), limit = 2).first()
        parse_header(header)
    }

    abstract fun parse_header(s: String)
    abstract fun updateUITitle()

    open fun parse(line: String, columns: Array<String>): Int {
        log.error("not implemented")
        return -1
    }

    /**
     * Set [.startOfGraph] and [.endOfGraph] to the given value if none are available yet
     * or update either of both, depending on if the given value is earlier/later than the formerly
     * stored corresponding one.
     *
     * @param nowStat Date/time of the currently parsed line.
     */
    protected fun setStartAndEndOfGraph(nowStat: LocalDateTime) {
        if (startOfGraph == null) {
            startOfGraph = nowStat
        }
        if (endOfGraph == null) {
            endOfGraph = nowStat
        }
        if (nowStat < startOfGraph) {
            startOfGraph = nowStat
        }
        if (nowStat > endOfGraph) {
            endOfGraph = nowStat
        }
    }

    val date: String?
        get() = if (sarStartDate == sarEndDate) {
            sarStartDate
        } else {
            "$sarStartDate to $sarEndDate"
        }

    fun setDate(s: String): Boolean {
        val currentDate: LocalDate
        val startDate: LocalDate
        val endDate: LocalDate
        if (sarStartDate == null) {
            sarStartDate = s
        }
        if (sarEndDate == null) {
            sarEndDate = s
        }
        try {
            val formatter = DateTimeFormatter.ofPattern(
                if ("Automatic Detection" == dateFormat) {
                    determineDateFormat(s)
                } else {
                    dateFormat
                }
            )
            log.debug("Date formatter: {}", formatter)
            currentDate = LocalDate.parse(s, formatter)
            startDate = LocalDate.parse(sarStartDate, formatter)
            endDate = LocalDate.parse(sarEndDate, formatter)
        } catch (ex: DateTimeParseException) {
            log.error("unable to parse date {}", s, ex)
            return false
        }
        parsedate = currentDate
        if (currentDate < startDate) {
            sarStartDate = s
        }
        if (currentDate > endDate) {
            sarEndDate = s
        }
        log.debug("parsedDate: {}, startDate: {}, EndDate: {}", currentDate, sarStartDate, sarEndDate)
        return true
    }
}
