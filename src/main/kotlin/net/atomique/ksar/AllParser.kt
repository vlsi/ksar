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

    private var dateFormatter: DateTimeFormatter? = null

    companion object {
        private val log = LoggerFactory.getLogger(AllParser::class.java)

        private val DATE_FORMATS: kotlin.collections.List<DateTimeFormatter> = listOf(
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
        ).map { DateTimeFormatter.ofPattern(it, Locale.US) }

        @JvmStatic
        fun determineDateFormat(dateString: String): DateTimeFormatter? {
            // sar records the past, so when an ambiguous date matches several patterns, prefer the
            // latest interpretation that is not in the future. For example 31/12/23 matches both
            // dd/MM/yy (2023-12-31) and yy/MM/dd (2031-12-23); the former is the right one. The small
            // margin tolerates clock skew and the time-zone gap between the host and this machine.
            val latestPlausible = LocalDateTime.now().plusHours(3).toLocalDate()
            var best: DateTimeFormatter? = null
            var bestDate: LocalDate? = null
            var bestPast: DateTimeFormatter? = null
            var bestPastDate: LocalDate? = null
            for (format in DATE_FORMATS) {
                try {
                    val nextDate = LocalDate.parse(dateString, format)
                    log.trace("'{}' is a valid {} date: {}", dateString, format, nextDate)
                    if (bestDate == null || nextDate >= bestDate) {
                        bestDate = nextDate
                        best = format
                    }
                    if (!nextDate.isAfter(latestPlausible) &&
                        (bestPastDate == null || nextDate >= bestPastDate)
                    ) {
                        log.trace("'{}' as {} gives a later non-future date: {}", dateString, format, nextDate)
                        bestPastDate = nextDate
                        bestPast = format
                    }
                } catch (e: DateTimeParseException) {
                    /* ignore */
                }
            }
            // Fall back to the overall latest match if every interpretation is in the future.
            return bestPast ?: best
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

    fun parse_header(s: String) {
        // Each header may carry its own date format, so drop the formatter detected for an
        // earlier header instead of reusing a stale one.
        dateFormatter = null
        parseHeader(s)
    }

    abstract fun parseHeader(s: String)
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
        val start = startOfGraph
        if (start == null || nowStat < start) {
            startOfGraph = nowStat
        }
        val end = endOfGraph
        if (end == null || nowStat > end) {
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
        val startStr = sarStartDate!!
        val endStr = sarEndDate!!
        try {
            val formatter = getDateFormatter(s) ?: run {
                log.error("unable to determine date format for {}", s)
                return false
            }
            log.debug("Date formatter: {}", formatter)
            currentDate = LocalDate.parse(s, formatter)
            startDate = LocalDate.parse(startStr, formatter)
            endDate = LocalDate.parse(endStr, formatter)
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

    private fun getDateFormatter(s: String): DateTimeFormatter? {
        dateFormatter?.let { return it }
        val format = if ("Automatic Detection" == dateFormat) {
            determineDateFormat(s)
        } else {
            DateTimeFormatter.ofPattern(dateFormat)
        }
        dateFormatter = format
        return format
    }
}
