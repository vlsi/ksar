/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.atomique.ksar.UI;

import java.util.Collections;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author Max
 */
public class SortedTreeNode extends DefaultMutableTreeNode implements Comparable {

    public static final long serialVersionUID = 15071L;


    public SortedTreeNode(String name) {
        super(name);
    }

    public SortedTreeNode(TreeNodeInfo tmp) {
        super(tmp);
    }

    public SortedTreeNode(ParentNodeInfo tmp) {
        super(tmp);
    }
    /*
    public SortedTreeNode (GraphDescription graphdesc) {
        super(graphdesc);
    }
    */

    public void count_graph(SortedTreeNode node) {
        int num = node.getChildCount();

        if (num > 0) {
            for (int i = 0; i < num; i++) {
                SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
                count_graph(l);
            }
        } else {
            Object obj1 = node.getUserObject();
            if (obj1 instanceof TreeNodeInfo) {
                leaf_num++;
            }
        }
        
    }
    
    public int  LeafCount() {
        leaf_num=0;
        count_graph(this);
        return leaf_num;
    }
    
    @Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
        super.insert(newChild, childIndex);
        Collections.sort(this.children);
    }
    public int compareTo(final Object o) {
        return this.toString().compareToIgnoreCase(o.toString());
    }

    private int leaf_num=0;
}
