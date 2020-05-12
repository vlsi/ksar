/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.export;

import static com.itextpdf.text.FontFactory.COURIER;
import static com.itextpdf.text.FontFactory.getFont;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.ExceptionConverter;
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

  private FilePDF(String filename, kSar hissar) {
    pdffilename = filename;
    mysar = hissar;
  }

  public FilePDF(String filename, kSar hissar, JProgressBar g, JDialog d) {
    this(filename, hissar);

    progress_bar = g;
    dialog = d;
  }

  public void run() {
    total_pages += mysar.get_page_to_print();

    switch (Config.getPDFPageFormat()) {

      case  "A4":
        document = new Document(PageSize.A4.rotate());
        break;

      case "LEGAL":
        document = new Document(PageSize.LEGAL.rotate());
        break;

      case "LETTER":
        document = new Document(PageSize.LETTER.rotate());
        break;

      default:
        document = new Document(PageSize.A4.rotate());
        break;
    }

    pdfheight = document.getPageSize().getHeight();
    pdfwidth = document.getPageSize().getWidth();
    pageheight = pdfheight - (2 * pdfmargins);
    pagewidth = pdfwidth - (2 * pdfmargins);

    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(pdffilename));

      writer.setPageEvent(this);
      writer.setCompressionLevel(0);

      // document parameter before open
      document.addTitle("kSar Grapher");
      //document.addSubject("SAR Statistics of " + mysar.hostName);
      //document.addKeywords("https://github.com/vlsi/ksar");
      //document.addKeywords(mysar.hostName);
      //document.addKeywords(mysar.myOS.sarStartDate);
      //document.addKeywords(mysar.myOS.sarEndDate);
      document.addCreator("kSar Version:" + VersionNumber.getVersionString());
      document.addAuthor("https://github.com/vlsi/ksar");

      // open the doc
      document.open();
      pdfcb = writer.getDirectContent();
      PdfOutline root = pdfcb.getRootOutline();

      IndexPage(document);

      export_treenode(mysar.graphtree, root);

      document.close();

    } catch (DocumentException | FileNotFoundException ex) {
      log.error("PDF creation Exception", ex);
    } finally {
      if (writer != null) {
        writer.close();
      }
    }

    if (dialog != null) {
      dialog.dispose();
    }


  }

  private void export_treenode(SortedTreeNode node, PdfOutline root) {
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
          new PdfOutline(root, new PdfDestination(PdfDestination.FIT), nodeobj.getTitle());
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

      int pageNumber = writer.getPageNumber();
      String text = "Page " + pageNumber + "/" + total_pages;
      String hostName = mysar.myparser.gethostName();
      String date = mysar.myparser.getDate();

      pdfcb.beginText();
      pdfcb.setFontAndSize(bf, 10);
      pdfcb.setColorFill(new BaseColor(0x00, 0x00, 0x00));

      if ( pageNumber > 1) {
        pdfcb.showTextAligned(PdfContentByte.ALIGN_LEFT, hostName, pdfmargins, pdfheight - pdfmargins, 0);
        pdfcb.showTextAligned(PdfContentByte.ALIGN_RIGHT, date, pdfwidth - pdfmargins, pdfheight - pdfmargins, 0);
      }

      pdfcb.showTextAligned(PdfContentByte.ALIGN_RIGHT, text,pdfwidth - pdfmargins,pdfmargins - 5 ,0);
      pdfcb.endText();
    } catch (Exception e) {
      throw new ExceptionConverter(e);
    }
  }

  private void addchart(PdfWriter writer, Graph graph) {
    JFreeChart chart =
        graph.getgraph(mysar.myparser.getStartOfGraph(), mysar.myparser.getEndOfGraph());
    PdfTemplate pdftpl = pdfcb.createTemplate(pagewidth, pageheight);
    Graphics2D g2d = new PdfGraphics2D(pdftpl, pagewidth, pageheight);
    Double r2d = new Rectangle2D.Double(0, 0, pagewidth, pageheight);
    chart.draw(g2d, r2d);
    g2d.dispose();
    pdfcb.addTemplate(pdftpl, pdfmargins, pdfmargins);
    try {
      writer.releaseTemplate(pdftpl);
    } catch (IOException ioe) {
      log.error("Unable to write to : {}", pdffilename);
    }
  }

  private void IndexPage(Document document) {
    try {
      float pdfCenter = ((pdfwidth - pdfmargins) / 2 );

      String title = "SAR Statistics";
      String t_date = "on " + mysar.myparser.getDate();
      String hostName = "for " + mysar.myparser.gethostName();
      String osType = mysar.myparser.getOstype();
      String graphStart = mysar.myparser.getStartOfGraph().toString();
      String graphEnd = mysar.myparser.getEndOfGraph().toString();

      pdfcb.beginText();
      pdfcb.setFontAndSize(bf, 40);
      pdfcb.setColorFill(new BaseColor(0x00, 0x00, 0x00));
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, title, pdfCenter, 500, 0);

      pdfcb.setFontAndSize(bf, 32);
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, hostName + " (" + osType + ")", pdfCenter, 400,0);
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, t_date, pdfCenter, 300,0);

      pdfcb.setFontAndSize(bf, 20);
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, graphStart, pdfCenter, 200,0);
      pdfcb.showTextAligned(PdfContentByte.ALIGN_CENTER, graphEnd, pdfCenter, 150,0);

      pdfcb.endText();
      document.newPage();

    } catch (Exception de) {
      log.error("IndexPage Exception", de);
    }
  }

  private int progress_info = 0;
  private float pdfwidth;
  private float pdfheight;
  private int pdfmargins = 10;
  private float pageheight;
  private float pagewidth;
  private int total_pages = 1; // page 1 (index)
  private String pdffilename;
  private Document document = null;
  private PdfWriter writer = null;
  private PdfContentByte pdfcb;
  private kSar mysar;
  private BaseFont bf = getFont(COURIER).getCalculatedBaseFont(false);
  private JProgressBar progress_bar = null;
  private JDialog dialog = null;
}
