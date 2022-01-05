/*
 * Copyright 2022 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.ui;

import java.util.Comparator;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class SortedTreeNode extends DefaultMutableTreeNode implements Comparable<SortedTreeNode> {

  public static final long serialVersionUID = 15071L;

  private static final Comparator<String> comparator =
      Comparator.nullsFirst(
          Comparator.<String, Boolean>comparing("all"::equals)
              .thenComparing("sum"::equals)
              .thenComparing("lo"::equals)
              .reversed()
              .thenComparing(NaturalComparator.INSTANCE));

  public SortedTreeNode(String name) {
    super(name);
  }

  public SortedTreeNode(TreeNodeInfo tmp) {
    super(tmp);
  }

  public SortedTreeNode(ParentNodeInfo tmp) {
    super(tmp);
  }

  @Override
  public void insert(final MutableTreeNode newChild, final int childIndex) {
    super.insert(newChild, childIndex);
    this.children.sort(null);
  }

  public int compareTo(final SortedTreeNode o) {
    return comparator.compare(this.toString(), o.toString());
  }

}
