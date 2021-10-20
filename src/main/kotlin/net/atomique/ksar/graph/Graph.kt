/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.graph

import net.atomique.ksar.Config
import net.atomique.ksar.kSar
import net.atomique.ksar.xml.GraphConfig
import net.atomique.ksar.ui.SortedTreeNode
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeTableXYDataset
import java.time.LocalDateTime
import org.jfree.data.time.Second
import java.lang.NumberFormatException
import java.lang.ArrayIndexOutOfBoundsException
import org.jfree.data.general.SeriesException
import net.atomique.ksar.GlobalOptions
import java.lang.StringBuilder
import org.jfree.data.time.RegularTimePeriod
import org.jfree.chart.ChartUtils
import java.io.IOException
import javax.swing.JCheckBox
import org.jfree.chart.JFreeChart
import org.jfree.data.xy.XYDataset
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.CombinedDomainXYPlot
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2
import org.jfree.chart.plot.XYPlot
import java.awt.Color
import java.awt.BasicStroke
import org.jfree.chart.renderer.xy.StandardXYItemRenderer
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.axis.DateAxis
import java.util.HashMap
import java.awt.event.ItemEvent
import net.atomique.ksar.ui.TreeNodeInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.util.ArrayList

class Graph(
    private val mysar: kSar,
    private val graphConfig: GraphConfig,
    val title: String,
    hdrs: String,
    private val firstDataColumn: Int,
    pp: SortedTreeNode?
) {
    companion object {
        private val log = LoggerFactory.getLogger(Graph::class.java)
    }

    private val axisOfDate = DateAxis("")
    private lateinit var mygraph: JFreeChart
    private var chartPanel: ChartPanel? = null

    var isPrintSelected = true
    private var printCheckBox = JCheckBox(this.title, isPrintSelected).apply {
        val checkBox = this
        addItemListener { evt: ItemEvent ->
            if (evt.source === checkBox) {
                isPrintSelected = checkBox.isSelected
            }
        }
    }

    private var headerStr = hdrs.split(Regex("\\s+"))
    private val stats = ArrayList<TimeSeries>()
    private val stackListByName: MutableMap<String, TimeTableXYDataset> = HashMap()
    private val stackListByCol: MutableMap<String, TimeTableXYDataset> = HashMap()

    init {
        if (pp != null) {
            mysar.add2tree(pp, SortedTreeNode(TreeNodeInfo(title, this)))
        }
        createDataStore()
    }

    private fun createDataStore() {
        // create timeseries
        for (i in firstDataColumn until headerStr.size) {
            stats.add(TimeSeries(headerStr[i]))
        }
        // create stack
        for (tmp in graphConfig.stacklist.values) {
            val dataSet = TimeTableXYDataset()
            val s = tmp.headerStr!!.split(Regex("\\s+"))
            for (value in s) {
                stackListByCol[value] = dataSet
            }
            stackListByName[tmp.title] = dataSet
        }
    }

    fun parse_line(ldt: LocalDateTime, s: String): Int {
        val now = Second(
            ldt.second,
            ldt.minute,
            ldt.hour,
            ldt.dayOfMonth,
            ldt.monthValue,
            ldt.year
        )
        parse_line(now, s)
        return 0
    }

    fun parse_line(now: Second, s: String): Int {
        val cols = s.split(Regex("\\s+"))
        var colvalue: Double
        // log.debug("graph parsing: {}", s);
        for (i in firstDataColumn until headerStr.size) {
            colvalue = try {
                cols[i].toDouble()
            } catch (ne: NumberFormatException) {
                log.error("{} {} is NaN", title, cols[i])
                return 0
            } catch (aie: ArrayIndexOutOfBoundsException) {
                log.error("{} col {} is missing {}", title, i, s)
                return 0
            } catch (ae: Exception) {
                log.error("{} {} is undef {}", title, cols[i], s)
                ae.printStackTrace()
                return 0
            }
            addDatapointPlot(now, i - firstDataColumn, headerStr[i - firstDataColumn], colvalue)
            val tmp = stackListByCol[headerStr[i]]
            if (tmp != null) {
                add_datapoint_stack(tmp, now, i, headerStr[i], colvalue)
            }
        }
        return 0
    }

    private fun add_datapoint_stack(
        dataset: TimeTableXYDataset,
        now: Second,
        col: Int,
        colHeader: String,
        value: Double
    ): Boolean {
        return try {
            dataset.add(now, value, colHeader)
            true
        } catch (se: SeriesException) {
            /*
       *
       * not update on stack
       *
      int indexcol = -1;
      // add not working
      // find timeseries index
      for (int i = 0; i < dataset.getSeriesCount(); i++) {
          String name = (String) dataset.getSeriesKey(i);
          if (colheader.equals(name)) {
              indexcol = i;
              break;
          }
          log.debug("{}: {}" , dataset.indexOf(name), name);
      }
      if (indexcol == -1) {
          return false;
      }
      StatConfig statconfig = ((OSParser) mysar.myparser).get_OSConfig().getStat(mysar.myparser.getCurrentStat());
      if (statconfig != null) {
          if (statconfig.canDuplicateTime()) {
              Number oldval = dataset.getXValue(indexcol, col);
              double tempval;
              if (oldval == null) {
                  return false;
              }
              ColumnConfig colconfig = GlobalOptions.getColumnConfig(colheader);
              if (colconfig.getType() == 1) {
                  tempval = ((oldval.doubleValue() + value) / 2);
              } else if (colconfig.getType() == 2) {
                  tempval = (oldval.doubleValue() + value);
              } else {
                  return false;
              }

              try {
                  ((TimeSeries) (Stats.get(col))).update(now, tempval);
                  return true;
              } catch (SeriesException se2) {
                  return false;
              }
          }
      }
      */
            false
        }
    }

    private fun addDatapointPlot(now: Second, col: Int, colHeader: String, value: Double): Boolean {
        return try {
            stats[col].add(now, value)
            true
        } catch (se: SeriesException) {
            // insert not possible
            // check if column can be update
            val statConfig = mysar.myparser.get_OSConfig().getStat(mysar.myparser.currentStat)
            if (statConfig != null) {
                if (statConfig.canDuplicateTime()) {
                    val oldVal = stats[col].getValue(now)
                    val tempVal: Double
                    if (oldVal == null) {
                        return false
                    }
                    val colConfig = GlobalOptions.getColumnConfig(colHeader) ?: return false
                    tempVal = if (colConfig.type == 1) {
                        (oldVal.toDouble() + value) / 2
                    } else if (colConfig.type == 2) {
                        oldVal.toDouble() + value
                    } else {
                        return false
                    }
                    return try {
                        stats[col].update(now, tempVal)
                        true
                    } catch (se2: SeriesException) {
                        false
                    }
                }
            }
            false
        }
    }

    fun make_csv(): String {
        val tmp = StringBuilder()
        tmp.append("Date;")
        tmp.append(csvHeader)
        tmp.append("\n")
        val dateList = stats[0]
        for (item in dateList.timePeriods) {
            tmp.append(item)
            tmp.append(";")
            tmp.append(getCsvLine(item as RegularTimePeriod))
            tmp.append("\n")
        }
        return tmp.toString()
    }

    val csvHeader: String
        get() {
            val tmp = StringBuilder()
            for (i in firstDataColumn until headerStr.size) {
                val tmpSeries = stats[i - firstDataColumn]
                tmp.append(title).append(" ").append(tmpSeries.key)
                tmp.append(";")
            }
            return tmp.toString()
        }

    fun getCsvLine(t: RegularTimePeriod?): String {
        val tmp = StringBuilder()
        for (i in firstDataColumn until headerStr.size) {
            val tmpSeries = stats[i - firstDataColumn]
            tmp.append(tmpSeries.getValue(t))
            tmp.append(";")
        }
        return tmp.toString()
    }

    fun savePNG(
        filename: String,
        width: Int,
        height: Int
    ): Int {
        try {
            ChartUtils.saveChartAsPNG(
                File(filename),
                getgraph(mysar.myparser.startOfGraph, mysar.myparser.endOfGraph), width,
                height
            )
        } catch (e: IOException) {
            log.error("Unable to write to : {}", filename)
            return -1
        }
        return 0
    }

    fun saveJPG(
        filename: String,
        width: Int,
        height: Int
    ): Int {
        try {
            ChartUtils.saveChartAsJPEG(
                File(filename),
                getgraph(mysar.myparser.startOfGraph, mysar.myparser.endOfGraph), width,
                height
            )
        } catch (e: IOException) {
            log.error("Unable to write to : {}", filename)
            return -1
        }
        return 0
    }

    fun getprintform(): JCheckBox = printCheckBox

    fun doPrint(): Boolean = isPrintSelected

    fun getgraph(start: LocalDateTime?, end: LocalDateTime?): JFreeChart {
        if (!::mygraph.isInitialized) {
            mygraph = makeGraph(start, end)!!
        } else {
            // TODO - get rid of Second, convert from LocalDateTime directly to java.util.Date - How to deal with required timezone in that case?
            val getStartOfGraphStart = convertLocalDateTimeToSecond(mysar.myparser.startOfGraph!!).start
            val getEndOfGraphEnd = convertLocalDateTimeToSecond(mysar.myparser.endOfGraph!!).end
            if (axisOfDate.minimumDate != getStartOfGraphStart) {
                axisOfDate.minimumDate = getStartOfGraphStart
            }
            if (axisOfDate.maximumDate != getEndOfGraphEnd) {
                axisOfDate.maximumDate = getEndOfGraphEnd
            }
        }
        return mygraph
    }

    private fun createCollection(list: kotlin.collections.List<String>): XYDataset? {
        val graphCollection = TimeSeriesCollection()
        var found: TimeSeries?
        var hasdata = false
        for (item in list) {
            found = null
            for (j in stats.indices) {
                found = stats[j]
                found = if (found.key == item) {
                    break
                } else {
                    null
                }
            }
            if (found != null) {
                graphCollection.addSeries(found)
                hasdata = true
            }
        }
        return graphCollection.takeIf { hasdata }
    }

    fun getChartPanel(): ChartPanel {
        if (chartPanel == null) {
            chartPanel = if (mysar.isParsing) {
                ChartPanel(getgraph(null, null))
            } else {
                ChartPanel(
                    getgraph(mysar.myparser.startOfGraph, mysar.myparser.endOfGraph)
                )
            }
        } else {
            if (!mysar.isParsing) {
                // TODO - get rid of Second, convert from LocalDateTime directly to java.util.Date - How to deal with required timezone in that case?
                val getStartOfGraphStart = convertLocalDateTimeToSecond(mysar.myparser.startOfGraph!!).start
                val getEndOfGraphEnd = convertLocalDateTimeToSecond(mysar.myparser.endOfGraph!!).end
                if (axisOfDate.minimumDate != getStartOfGraphStart) {
                    axisOfDate.minimumDate = getStartOfGraphStart
                }
                if (axisOfDate.maximumDate != getEndOfGraphEnd) {
                    axisOfDate.maximumDate = getEndOfGraphEnd
                }
            }
        }
        return chartPanel!!
    }

    private fun makeGraph(start: LocalDateTime?, end: LocalDateTime?): JFreeChart? {
        val beginGenerate = System.currentTimeMillis()
        val plot = CombinedDomainXYPlot(axisOfDate)
        // do the stacked stuff
        for (plotStackConfig in graphConfig.stacklist.values) {
            stackListByName[plotStackConfig.title]?.let { timeTableXYDataset ->
                val renderer = StackedXYAreaRenderer2()
                val graphAxisTitle = plotStackConfig.axis
                val tempPlot = XYPlot(timeTableXYDataset, axisOfDate, graphAxisTitle, renderer)
                for (i in 0 until timeTableXYDataset.seriesCount) {
                    val color = GlobalOptions.getDataColor(timeTableXYDataset.getSeriesKey(i).toString())
                    if (color != null) {
                        renderer.setSeriesPaint(i, color)
                        renderer.defaultStroke = BasicStroke(1.0f)
                    }
                }
                plot.add(tempPlot, plotStackConfig.size)
            }
        }
        // do the line stuff
        for (tmp in graphConfig.plotlist.values) {
            val renderer = StandardXYItemRenderer()
            val headers = tmp.headerStr!!.split(Regex("\\s+"))
            val c = createCollection(headers)
            val graphAxisTitle = tmp.axis
            val tmpPlot = XYPlot(c, axisOfDate, graphAxisTitle, renderer)
            for ((index, header) in headers.withIndex()) {
                val color = GlobalOptions.getDataColor(header)
                if (color != null) {
                    renderer.setSeriesPaint(index, color)
                    renderer.defaultStroke = BasicStroke(1.0f)
                }
            }
            plot.add(tmpPlot, tmp.size)
        }
        if (plot.subplots.isEmpty()) {
            return null
        }
        if (start != null && end != null) {
            val gStart = convertLocalDateTimeToSecond(start)
            val gEnd = convertLocalDateTimeToSecond(end)
            axisOfDate.setRange(gStart.start, gEnd.end)
        }
        plot.orientation = PlotOrientation.VERTICAL
        val myChart = JFreeChart(title, Config.dEFAULT_FONT, plot, true)
        val endGenerate = System.currentTimeMillis()
        myChart.backgroundPaint = Color.white
        log.debug("graph generation: {} ms", endGenerate - beginGenerate)
        return myChart
    }

    private fun convertLocalDateTimeToSecond(ldt: LocalDateTime): Second {
        val day = ldt.dayOfMonth
        val month = ldt.monthValue
        val year = ldt.year
        val hour = ldt.hour
        val minute = ldt.minute
        val second = ldt.second
        return Second(second, minute, hour, day, month, year)
    }
}
