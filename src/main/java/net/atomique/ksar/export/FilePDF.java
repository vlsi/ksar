/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.export;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ExceptionConverter;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import net.atomique.ksar.Config;
import net.atomique.ksar.VersionNumber;
import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.graph.List;
import net.atomique.ksar.kSar;
import net.atomique.ksar.ui.ParentNodeInfo;
import net.atomique.ksar.ui.SortedTreeNode;
import net.atomique.ksar.ui.TreeNodeInfo;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JProgressBar;

public class FilePDF extends PdfPageEventHelper implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FilePDF.class);

  public FilePDF(String filename, kSar hissar) {
    pdffilename = filename;
    mysar = hissar;

  }

  public FilePDF(String filename, kSar hissar, JProgressBar g, JDialog d) {
    pdffilename = filename;
    mysar = hissar;
    progress_bar = g;
    dialog = d;
  }

  public void run() {
    total_pages += mysar.get_page_to_print();
    org.jfree.text.TextUtilities.setUseDrawRotatedStringWorkaround(true);
    if ("A4".equals(Config.getPDFPageFormat())) {
      document = new Document(PageSize.A4.rotate());
      pdfheight = document.getPageSize().getHeight();
    } else if ("LEGAL".equals(Config.getPDFPageFormat())) {
      document = new Document(PageSize.LEGAL.rotate());
    } else if ("LETTER".equals(Config.getPDFPageFormat())) {
      document = new Document(PageSize.LETTER.rotate());
    } else {
      document = new Document(PageSize.A4.rotate());
    }
    pdfheight = document.getPageSize().getHeight();
    pdfwidth = document.getPageSize().getWidth();
    pageheight = pdfheight - (2 * pdfmargins);
    pagewidth = pdfwidth - (2 * pdfmargins);
    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(pdffilename));
    } catch (DocumentException | FileNotFoundException ex) {
      log.error("Parser Exception", ex);
    }
    writer.setPageEvent(this);
    writer.setCompressionLevel(0);

    // document parameter before open
    document.addTitle("kSar Grapher");
    //document.addSubject("Sar output of " + mysar.hostName);
    //document.addKeywords("http://ksar.atomique.net/ ");
    //document.addKeywords(mysar.hostName);
    //document.addKeywords(mysar.myOS.sarStartDate);
    //document.addKeywords(mysar.myOS.sarEndDate);
    document.addCreator("kSar Version:" + VersionNumber.getVersionNumber());
    document.addAuthor("Xavier cherif");

    // open the doc
    document.open();
    pdfcb = writer.getDirectContent();
    PdfOutline root = pdfcb.getRootOutline();

    IndexPage(writer, document);

    export_treenode(mysar.graphtree, root);

    document.close();

    if (dialog != null) {
      dialog.dispose();
    }


  }


  public void export_treenode(SortedTreeNode node, PdfOutline root) {
    int num = node.getChildCount();
    if (num > 0) {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
        if (nodeobj.isPrintSelected()) {
          root = new PdfOutline(root, new PdfDestination(PdfDestination.FIT), nodeobj.getTitle());
        }
      }
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        export_treenode(l, root);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        if (nodeobj.isPrintSelected()) {
          root = new PdfOutline(root, new PdfDestination(PdfDestination.FIT), nodeobj.getTitle());
          update_ui();
          addchart(writer, nodeobj);
          document.newPage();

        }
      }
    }
  }

  private void update_ui() {
    if (progress_bar != null) {
      progress_bar.setValue(++progress_info);
      progress_bar.repaint();
    }

  }

  public void onEndPage(PdfWriter writer, Document document) {
    try {
      String text = "Page " + writer.getPageNumber() + "/" + total_pages;

      pdfcb.beginText();
      pdfcb.setFontAndSize(bf, 10);
      pdfcb.setColorFill(new BaseColor(0x00, 0x00, 0x00));
      pdfcb.showTextAligned(PdfContentByte.ALIGN_RIGHT, text, ((pdfwidth - pdfmargins) - 10),
          10 + pdfmargins, 0);
      pdfcb.endText();
    } catch (Exception e) {
      throw new ExceptionConverter(e);
    }
  }

  public int addchart(PdfWriter writer, Graph graph) {
    JFreeChart chart =
        graph.getgraph(mysar.myparser.get_startofgraph(), mysar.myparser.get_endofgraph());
    PdfTemplate pdftpl = pdfcb.createTemplate(pagewidth, pageheight);
    Graphics2D g2d = new PdfGraphics2D(pdftpl, pagewidth, pageheight);
    Double r2d = new Rectangle2D.Double(0, 0, pagewidth, pageheight);
    chart.draw(g2d, r2d, chartinfo);
    g2d.dispose();
    pdfcb.addTemplate(pdftpl, pdfmargins, pdfmargins);
    try {
      writer.releaseTemplate(pdftpl);
    } catch (IOException ioe) {
      log.error("Unable to write to : {}", pdffilename);
    }
    return 0;
  }

  public void IndexPage(PdfWriter writer, Document document) {
    try {

      String title = "Statistics";
      String t_date = "On " + mysar.myparser.getDate();
      pdfcb.beginText();
      pdfcb.setFontAndSize(bf, 48);
      pdfcb.setColorFill(new BaseColor(0x00, 0x00, 0x00));
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, title, ((pdfwidth - pdfmargins) / 2), 500,
          0);
      pdfcb.setFontAndSize(bf, 36);
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, t_date, ((pdfwidth - pdfmargins) / 2), 300,
          0);
      pdfcb.endText();
      document.newPage();

    } catch (Exception de) {
      log.error("IndexPage Exception", de);
    }
  }

  private int progress_info = 0;
  private float pdfheight;
  private float pdfwidth;
  private int pdfmargins = 10;
  float pageheight;
  float pagewidth;
  private int total_pages = 1; // page 1 (index)
  private String pdffilename = null;
  private Document document = null;
  private PdfWriter writer = null;
  private PdfContentByte pdfcb;
  private kSar mysar = null;
  FontMapper mapper = new DefaultFontMapper();
  BaseFont bf = FontFactory.getFont(FontFactory.COURIER).getCalculatedBaseFont(false);
  private JProgressBar progress_bar = null;
  private JDialog dialog = null;
  private ChartRenderingInfo chartinfo = null;
}
