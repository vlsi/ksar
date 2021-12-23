/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.ui

import net.atomique.ksar.graph.List

class ParentNodeInfo(var node_title: String, var node_object: List) {
    override fun toString(): String = node_title
}
