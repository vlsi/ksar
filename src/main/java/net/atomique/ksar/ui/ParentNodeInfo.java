/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.ui;

import net.atomique.ksar.graph.List;

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
