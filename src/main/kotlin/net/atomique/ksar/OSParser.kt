/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

import net.atomique.ksar.xml.OSConfig
import org.slf4j.LoggerFactory
import java.lang.StringBuilder
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.DateTimeException
import java.util.HashMap

abstract class OSParser(hissar: kSar, header: String) : AllParser(hissar, header) {
    protected var ListofGraph: MutableMap<String, Any> = HashMap()
    protected var lastStat: String? = null
    protected var currentStatObj: Any? = null
    var ostype: String? = null
    protected var Hostname: String? = null
    protected var OSversion: String? = null
    protected var Kernel: String? = null
    protected var CpuType: String? = null
    protected var MacAddress: String? = null
    protected var Memory: String? = null
    protected var NBDisk: String? = null
    protected var NBCpu: String? = null
    protected var ENT: String? = null

    companion object {
        private val log = LoggerFactory.getLogger(OSParser::class.java)
    }

    init {
        init(header)
    }

    override fun init(header: String) {
        super.init(header)
        myosconfig = GlobalOptions.getOSinfo(parserName)
    }

    fun get_OSConfig(): OSConfig {
        return myosconfig
    }

    override val info: String get() {
        val tmpstr = StringBuilder()
        tmpstr.append("OS Type: ").append(ostype)
        if (OSversion != null) {
            tmpstr.append("OS Version: ").append(OSversion).append("\n")
        }
        if (Kernel != null) {
            tmpstr.append("Kernel Release: ").append(Kernel).append("\n")
        }
        if (CpuType != null) {
            tmpstr.append("CPU Type: ").append(CpuType).append("\n")
        }
        if (Hostname != null) {
            tmpstr.append("Hostname: ").append(Hostname).append("\n")
        }
        if (MacAddress != null) {
            tmpstr.append("Mac Address: ").append(MacAddress).append("\n")
        }
        if (Memory != null) {
            tmpstr.append("Memory: ").append(Memory).append("\n")
        }
        if (NBDisk != null) {
            tmpstr.append("Number of disks: ").append(NBDisk).append("\n")
        }
        if (NBCpu != null) {
            tmpstr.append("Number of CPU: ").append(NBCpu).append("\n")
        }
        if (ENT != null) {
            tmpstr.append("Ent: ").append(ENT).append("\n")
        }
        if (sarStartDate != null) {
            tmpstr.append("Start of SAR: ").append(sarStartDate).append("\n")
        }
        if (sarEndDate != null) {
            tmpstr.append("End of SAR: ").append(sarEndDate).append("\n")
        }
        tmpstr.append("\n")
        return tmpstr.toString()
    }

    fun gethostName(): String? {
        return Hostname
    }

    override fun updateUITitle() {
        if (mysar.dataView != null) {
            var asFormattedDateTimeStart: String? = null
            var asFormattedDateTimeEnd: String? = null
            try {

                // Locale test = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) // .withLocale(test);
                startOfGraph?.let {
                    asFormattedDateTimeStart = it.format(formatter)
                }
                endOfGraph?.let {
                    // asFormattedDateTimeEnd = it.format(DateTimeFormatter.ISO_DATE_TIME);
                    asFormattedDateTimeEnd = it.format(formatter)
                }
            } catch (ex: DateTimeException) {
                log.error("unable to format time", ex)
            }
            if (asFormattedDateTimeStart != null && asFormattedDateTimeEnd != null) {
                mysar.dataView.title = String.format(
                    "%s from %s to %s", Hostname, asFormattedDateTimeStart,
                    asFormattedDateTimeEnd
                )
            }
        }
    }
}
