/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.atomique.ksar.UI;

import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;

/**
 *
 * @author Max
 */
public class ParentNodeInfo {

    public ParentNodeInfo(String t, List list) {
        node_title = t;
        node_object = list;
    }

    public List getNode_object() {
        return node_object;
    }

    public String getNode_title() {
        return node_title;
    }

    public String toString() {
        return node_title;
    }
    
    private String node_title = null;
    private List node_object = null;
    
}
