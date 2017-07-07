/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.export;

import net.atomique.ksar.GlobalOptions;
import net.atomique.ksar.graph.Graph;
import net.atomique.ksar.graph.List;
import net.atomique.ksar.kSar;
import net.atomique.ksar.ui.ParentNodeInfo;
import net.atomique.ksar.ui.SortedTreeNode;
import net.atomique.ksar.ui.TreeNodeInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import java.io.File;
import java.io.IOException;

public class FilePNG {
  public static int addchart(String path, Graph graph, kSar mysar) {
    int pagewidth = GlobalOptions.getWidth();
    int pageheight = GlobalOptions.getHeigth();
    JFreeChart chart = graph.getgraph(mysar.myparser.get_startofgraph(), mysar.myparser.get_endofgraph());
    try {
      ChartUtilities.saveChartAsPNG(new File(path), chart, pagewidth, pageheight);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return 0;
  }

  public static void drawCharts(SortedTreeNode node, kSar mysar) {
    int num = node.getChildCount();
    if (num > 0) {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof ParentNodeInfo) {
        ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
        List nodeobj = tmpnode.getNode_object();
      }
      for (int i = 0; i < num; i++) {
        SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
        drawCharts(l, mysar);
      }
    } else {
      Object obj1 = node.getUserObject();
      if (obj1 instanceof TreeNodeInfo) {
        TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
        Graph nodeobj = tmpnode.getNode_object();
        if (nodeobj.printSelected) {
          String name = nodeobj.getTitle().replace('/', '-');
          addchart(GlobalOptions.getOutIMG() + name + ".png", nodeobj, mysar);
        }

      }
    }
  }
}
