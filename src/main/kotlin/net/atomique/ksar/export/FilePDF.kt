/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.export

import net.atomique.ksar.kSar
import com.itextpdf.text.pdf.PdfPageEventHelper
import java.lang.Runnable
import javax.swing.JProgressBar
import javax.swing.JDialog
import com.itextpdf.text.pdf.PdfWriter
import java.io.FileOutputStream
import net.atomique.ksar.VersionNumber
import com.itextpdf.text.pdf.PdfOutline
import java.io.FileNotFoundException
import net.atomique.ksar.ui.SortedTreeNode
import net.atomique.ksar.ui.ParentNodeInfo
import com.itextpdf.text.pdf.PdfDestination
import net.atomique.ksar.ui.TreeNodeInfo
import com.itextpdf.text.pdf.PdfContentByte
import java.awt.Graphics2D
import com.itextpdf.awt.PdfGraphics2D
import com.itextpdf.text.*
import java.io.IOException
import net.atomique.ksar.Config
import net.atomique.ksar.graph.Graph
import org.slf4j.LoggerFactory
import java.awt.geom.Rectangle2D
import java.lang.Exception

class FilePDF private constructor(private val pdffilename: String, private val mysar: kSar) :
    PdfPageEventHelper(),
    Runnable {

    private var progressInfo = 0
    private var pdfwidth = 0f
    private var pdfheight = 0f
    private val pdfmargins = 10
    private var pageheight = 0f
    private var pagewidth = 0f
    private var totalPages = 1 // page 1 (index)
    private lateinit var document: Document
    private lateinit var writer: PdfWriter
    private lateinit var pdfcb: PdfContentByte
    private val bf = FontFactory.getFont(FontFactory.COURIER).getCalculatedBaseFont(false)
    private var progressBar: JProgressBar? = null
    private var dialog: JDialog? = null

    companion object {
        private val log = LoggerFactory.getLogger(FilePDF::class.java)
    }

    constructor(filename: String, hissar: kSar, g: JProgressBar?, d: JDialog?) : this(filename, hissar) {
        progressBar = g
        dialog = d
    }

    override fun run() {
        totalPages += mysar.get_page_to_print()
        document = when (Config.pDFPageFormat) {
            "A4" -> Document(PageSize.A4.rotate())
            "LEGAL" -> Document(PageSize.LEGAL.rotate())
            "LETTER" -> Document(PageSize.LETTER.rotate())
            else -> Document(PageSize.A4.rotate())
        }
        pdfheight = document.pageSize.height
        pdfwidth = document.pageSize.width
        pageheight = pdfheight - 2 * pdfmargins
        pagewidth = pdfwidth - 2 * pdfmargins
        try {
            writer = PdfWriter.getInstance(document, FileOutputStream(pdffilename))
            writer.pageEvent = this
            writer.compressionLevel = 0

            // document parameter before open
            document.addTitle("kSar Grapher")
            // document.addSubject("SAR Statistics of " + mysar.hostName);
            // document.addKeywords("https://github.com/vlsi/ksar");
            // document.addKeywords(mysar.hostName);
            // document.addKeywords(mysar.myOS.sarStartDate);
            // document.addKeywords(mysar.myOS.sarEndDate);
            document.addCreator("kSar Version:" + VersionNumber.versionString)
            document.addAuthor("https://github.com/vlsi/ksar")

            // open the doc
            document.open()
            pdfcb = writer.directContent
            val root = pdfcb.rootOutline
            indexPage(document)
            exportTreeNode(mysar.graphtree, root)
            document.close()
        } catch (ex: DocumentException) {
            log.error("PDF creation Exception", ex)
        } catch (ex: FileNotFoundException) {
            log.error("PDF creation Exception", ex)
        } finally {
            if (::writer.isInitialized) {
                writer.close()
            }
        }
        dialog?.dispose()
    }

    private fun exportTreeNode(node: SortedTreeNode, root: PdfOutline) {
        var root = root
        val num = node.childCount
        if (num > 0) {
            val obj1 = node.userObject
            if (obj1 is ParentNodeInfo) {
                val nodeObj = obj1.node_object
                if (nodeObj.isPrintSelected) {
                    root = PdfOutline(root, PdfDestination(PdfDestination.FIT), nodeObj.title)
                }
            }
            for (i in 0 until num) {
                val l = node.getChildAt(i) as SortedTreeNode
                exportTreeNode(l, root)
            }
        } else {
            val obj1 = node.userObject
            if (obj1 is TreeNodeInfo) {
                val nodeObj = obj1.node_object
                if (nodeObj.isPrintSelected) {
                    PdfOutline(root, PdfDestination(PdfDestination.FIT), nodeObj.title)
                    updateUi()
                    addChart(nodeObj)
                    document.newPage()
                }
            }
        }
    }

    private fun updateUi() {
        progressBar?.let {
            it.value = ++progressInfo
            it.repaint()
        }
    }

    override fun onEndPage(writer: PdfWriter, document: Document) {
        try {
            val pageNumber = writer.pageNumber
            val text = "Page $pageNumber/$totalPages"
            val parser = mysar.myparser
            val hostName = parser.gethostName()
            val date = parser.date
            pdfcb.beginText()
            pdfcb.setFontAndSize(bf, 10f)
            pdfcb.setColorFill(BaseColor(0x00, 0x00, 0x00))
            if (pageNumber > 1) {
                pdfcb.showTextAligned(
                    PdfContentByte.ALIGN_LEFT,
                    hostName,
                    pdfmargins.toFloat(),
                    pdfheight - pdfmargins,
                    0f
                )
                pdfcb.showTextAligned(
                    PdfContentByte.ALIGN_RIGHT,
                    date,
                    pdfwidth - pdfmargins,
                    pdfheight - pdfmargins,
                    0f
                )
            }
            pdfcb.showTextAligned(
                PdfContentByte.ALIGN_RIGHT,
                text,
                pdfwidth - pdfmargins,
                (pdfmargins - 5).toFloat(),
                0f
            )
            pdfcb.endText()
        } catch (e: Exception) {
            throw ExceptionConverter(e)
        }
    }

    private fun addChart(graph: Graph) {
        val chart = graph.getgraph(mysar.myparser.startOfGraph, mysar.myparser.endOfGraph)
        val pdftpl = pdfcb.createTemplate(pagewidth, pageheight)
        val g2d: Graphics2D = PdfGraphics2D(pdftpl, pagewidth, pageheight)
        val r2d = Rectangle2D.Double(0.0, 0.0, pagewidth.toDouble(), pageheight.toDouble())
        chart.draw(g2d, r2d)
        g2d.dispose()
        pdfcb.addTemplate(pdftpl, pdfmargins.toFloat(), pdfmargins.toFloat())
        try {
            writer.releaseTemplate(pdftpl)
        } catch (ioe: IOException) {
            log.error("Unable to write to : {}", pdffilename)
        }
    }

    private fun indexPage(document: Document) {
        try {
            val pdfCenter = (pdfwidth - pdfmargins) / 2
            val title = "SAR Statistics"
            val parser = mysar.myparser
            val tDate = "on " + parser.date
            val hostName = "for " + parser.gethostName()
            val osType = parser.ostype
            val graphStart = parser.startOfGraph.toString()
            val graphEnd = parser.endOfGraph.toString()
            pdfcb.beginText()
            pdfcb.setFontAndSize(bf, 40f)
            pdfcb.setColorFill(BaseColor(0x00, 0x00, 0x00))
            pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, title, pdfCenter, 500f, 0f)
            pdfcb.setFontAndSize(bf, 32f)
            pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, "$hostName ($osType)", pdfCenter, 400f, 0f)
            pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, tDate, pdfCenter, 300f, 0f)
            pdfcb.setFontAndSize(bf, 20f)
            pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, graphStart, pdfCenter, 200f, 0f)
            pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, graphEnd, pdfCenter, 150f, 0f)
            pdfcb.endText()
            document.newPage()
        } catch (de: Exception) {
            log.error("IndexPage Exception", de)
        }
    }
}
