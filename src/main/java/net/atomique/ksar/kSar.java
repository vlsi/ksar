/*
 * Copyright 2008 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.ui.DataView;
import net.atomique.ksar.ui.SortedTreeNode;
import net.atomique.ksar.ui.TreeNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JDesktopPane;

public class kSar {

  private static final Logger log = LoggerFactory.getLogger(kSar.class);

  public kSar(JDesktopPane DesktopPane) {
    dataview = new DataView(this);
    dataview.toFront();
    dataview.setVisible(true);
    dataview.setTitle("Empty");
    DesktopPane.add(dataview);
    try {
      int num = DesktopPane.getAllFrames().length;
      if (num != 1) {
        dataview.reshape(5 * num, 5 * num, 800, 600);
      } else {
        dataview.reshape(0, 0, 800, 600);
      }
      dataview.setSelected(true);
    } catch (PropertyVetoException vetoe) {
      log.error("PropertyVetoException", vetoe);
    }
    if (GlobalOptions.getCLfilename() != null) {
      do_fileread(GlobalOptions.getCLfilename());
    }
  }

  public kSar() {
  }

  public void do_fileread(String filename) {
    if (filename == null) {
      launched_action = new FileRead(this);
    } else {
      launched_action = new FileRead(this, filename);
    }
    reload_action = ((FileRead) launched_action).get_action();
    do_action();
  }

  public void do_localcommand(String cmd) {
    if (cmd == null) {
      launched_action = new LocalCommand(this);
    } else {
      launched_action = new LocalCommand(this, cmd);
    }
    reload_action = ((LocalCommand) launched_action).get_action();
    do_action();
  }

  public void do_sshread(String cmd) {
    if (cmd == null) {
      launched_action = new SSHCommand(this);
      //mysar.reload_command=t.get_command();
    } else {
      launched_action = new SSHCommand(this, cmd);
    }

    reload_action = ((SSHCommand) launched_action).get_action();
    do_action();
  }

  private void do_action() {
    if (reload_action == null) {
      log.info("action is null");
      return;
    }
    if (launched_action != null) {
      if (dataview != null) {
        dataview.notifyrun(true);
      }
      launched_action.start();
    }
  }

  public int parse(BufferedReader br) {
    String current_line;
    long parsing_start;
    long parsing_end;
    String[] columns;
    int parser_return;

    parsing_start = System.currentTimeMillis();

    try {
      while ((current_line = br.readLine()) != null && !action_interrupted) {
        Parsing = true;

        lines_parsed++;
        if (current_line.length() == 0) {
          continue;
        }
        columns = current_line.split("\\s+");

        if (columns.length == 0) {
          continue;
        }

        //log.debug("Header Line : {}", current_line);
        String firstColumn = columns[0];

        try {
          Class<?> classtmp = GlobalOptions.getParser(firstColumn);
          if (classtmp != null) {
            if (myparser == null) {
              myparser = (OSParser) classtmp.getDeclaredConstructor().newInstance();
              myparser.init(this, current_line);

              continue;
            } else {
              if (myparser.getParserName().equals(columns[0])) {
                myparser.parse_header(current_line);
                continue;
              }
            }
          }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
          log.error("Parser Exception", ex);
        }


        if (myparser == null) {
          log.error("unknown parser");
          Parsing = false;
          return -1;
        }

        parser_return = myparser.parse(current_line, columns);
        if (parser_return == 1 && GlobalOptions.isDodebug()) {
          log.trace("### {}", current_line);
        }
        if (parser_return < 0 && GlobalOptions.isDodebug()) {
          log.trace("ERR {}", current_line);
        }

        myparser.updateUITitle();
      }
    } catch (IOException ex) {
      log.error("IO Exception", ex);
      Parsing = false;
    }

    if (dataview != null) {
      dataview.treehome();
      dataview.notifyrun(false);
      dataview.setHasData(true);
    }

    parsing_end = System.currentTimeMillis();
    if (GlobalOptions.isDodebug()) {
      log.trace("time to parse: {} ms", (parsing_end - parsing_start));
      log.trace("lines parsed: {}", lines_parsed);
      if (myparser != null) {
        log.trace("number of datesamples: {}", myparser.DateSamples.size());
      }
    }
    Parsing = false;
    return -1;
  }

  void cleared() {
    aborted();
  }

  private void aborted() {
    if (dataview != null) {
      log.trace("reset menu");
      dataview.notifyrun(false);
    }
  }

  public void interrupt_parsing() {
    if (isParsing()) {
      action_interrupted = true;
    }
  }

  public void add2tree(SortedTreeNode parent, SortedTreeNode newNode) {
    if (dataview != null) {
      dataview.add2tree(parent, newNode);
    }
  }

  public int get_page_to_print() {
    page_to_print = 0;
    count_printSelected(graphtree);
    return page_to_print;
  }

  private void count_printSelected(SortedTreeNode node) {
    int num = node.getChildCount();

    if (num > 0) {
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        count_printSelected(l);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        if (nodeobj.isPrintSelected()) {
          page_to_print++;
        }
      }
    }
  }

  DataView getDataView() {
    return dataview;
  }

  public boolean isParsing() {
    return Parsing;
  }

  private DataView dataview = null;
  private long lines_parsed;
  private String reload_action = "Empty";
  private Thread launched_action = null;
  private boolean action_interrupted = false;
  public OSParser myparser = null;
  private boolean Parsing = false;
  public SortedTreeNode graphtree = new SortedTreeNode("kSar");
  private int page_to_print = 0;
}
