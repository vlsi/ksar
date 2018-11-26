/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.export;

import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.graph.List;
import net.atomique.ksar.kSar;
import net.atomique.ksar.ui.ParentNodeInfo;
import net.atomique.ksar.ui.SortedTreeNode;
import net.atomique.ksar.ui.TreeNodeInfo;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JProgressBar;

public class FileCSV implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(FileCSV.class);

  public FileCSV(String filename, kSar hissar) {
    csvfilename = filename;
    mysar = hissar;
  }

  public FileCSV(String filename, kSar hissar, JProgressBar g, JDialog d) {
    this(filename, hissar);

    progress_bar = g;
    dialog = d;
  }

  public void run() {
    // open file
    BufferedWriter out;
    try {
      out = new BufferedWriter(new FileWriter(csvfilename));
    } catch (IOException ex) {
      log.error("IO Exception", ex);
      out = null;
    }

    // print header
    tmpcsv.append("Date;");

    export_treenode_header(mysar.graphtree);
    tmpcsv.append("\n");
    Iterator<LocalDateTime> ite = mysar.myparser.getDateSamples().iterator();
    while (ite.hasNext()) {
      LocalDateTime tmpLDT = ite.next();

      Second tmp = new Second(tmpLDT.getSecond(),
          tmpLDT.getMinute(),
          tmpLDT.getHour(),
          tmpLDT.getDayOfMonth(),
          tmpLDT.getMonthValue(),
          tmpLDT.getYear());

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
      String text = tmpLDT.format(formatter);
      tmpcsv.append(text + ";");
      export_treenode_data(mysar.graphtree, tmp);
      tmpcsv.append("\n");
    }
    try {
      out.write(tmpcsv.toString());
      out.close();
    } catch (IOException ex) {
      log.error("IO Exception", ex);
    }


    if (dialog != null) {
      dialog.dispose();
    }

  }

  private void export_treenode_header(SortedTreeNode node) {
    int num = node.getChildCount();

    if (num > 0) {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        export_treenode_header(l);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        if (nodeobj.doPrint()) {
          tmpcsv.append(nodeobj.getCsvHeader());

        }
      }
    }
  }

  private void export_treenode_data(SortedTreeNode node, RegularTimePeriod time) {
    int num = node.getChildCount();

    if (num > 0) {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        export_treenode_data(l, time);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        if (nodeobj.doPrint()) {
          tmpcsv.append(nodeobj.getCsvLine(time));
          update_ui();

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

  private StringBuilder tmpcsv = new StringBuilder();
  private int progress_info = 0;
  private String csvfilename;
  private kSar mysar;
  private JProgressBar progress_bar = null;
  private JDialog dialog = null;
}
