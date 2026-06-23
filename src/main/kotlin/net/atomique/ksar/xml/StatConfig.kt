/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar.xml

class StatConfig(var statName: String) {
    var graphName: String? = null
    var headerStr: String? = null
    private var duplicatetime = false

    fun canDuplicateTime(): Boolean = duplicatetime

    fun setDuplicateTime(s: String) {
        if ("yes" == s || "true" == s) {
            duplicatetime = true
        }
    }
}
