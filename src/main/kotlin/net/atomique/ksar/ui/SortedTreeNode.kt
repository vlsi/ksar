/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.ui

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import java.util.*

class SortedTreeNode : DefaultMutableTreeNode, Comparable<Any?> {
    private var leaf_num = 0

    companion object {
        const val serialVersionUID = 15071L
        private val comparator = Comparator.nullsFirst(
            Comparator.comparing { v: String -> "all" == v }
                .thenComparing { v -> "sum" == v }
                .reversed()
                .thenComparing(NaturalComparator)
        )
    }

    constructor(name: String?) : super(name) {}
    constructor(tmp: TreeNodeInfo?) : super(tmp) {}
    constructor(tmp: ParentNodeInfo?) : super(tmp) {}

    /*
  public SortedTreeNode (GraphDescription graphdesc) {
      super(graphdesc);
  }
  */
    fun count_graph(node: SortedTreeNode) {
        val num = node.childCount
        if (num > 0) {
            for (i in 0 until num) {
                val l = node.getChildAt(i) as SortedTreeNode
                count_graph(l)
            }
        } else {
            val obj1 = node.getUserObject()
            if (obj1 is TreeNodeInfo) {
                leaf_num++
            }
        }
    }

    fun LeafCount(): Int {
        leaf_num = 0
        count_graph(this)
        return leaf_num
    }

    override fun insert(newChild: MutableTreeNode, childIndex: Int) {
        super.insert(newChild, childIndex)
        (children as Vector<SortedTreeNode>).sort()
    }

    override fun compareTo(other: Any?): Int {
        return comparator.compare(this.toString(), other.toString())
    }
}
