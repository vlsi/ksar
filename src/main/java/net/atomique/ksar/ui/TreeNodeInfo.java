/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.ui;

import net.atomique.ksar.graph.Graph;

public class TreeNodeInfo {

  public TreeNodeInfo(String t, Graph graph) {
    node_title = t;
    node_object = graph;
  }

  public Graph getNode_object() {
    return node_object;
  }

  public String getNode_title() {
    return node_title;
  }

  public String toString() {
    return node_title;
  }

  private String node_title = null;
  private Graph node_object = null;

}
