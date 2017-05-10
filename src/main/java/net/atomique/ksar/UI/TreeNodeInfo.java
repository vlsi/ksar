/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.atomique.ksar.UI;

import net.atomique.ksar.graph.Graph;

/**
 *
 * @author Max
 */
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
