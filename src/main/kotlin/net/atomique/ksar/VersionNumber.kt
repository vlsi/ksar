/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */
package net.atomique.ksar

object VersionNumber {
    @JvmStatic
    var versionString: String =
        VersionNumber::class.java.classLoader.getResourceAsStream("kSar.version")
            ?.bufferedReader()?.readText() ?: "unknown"
}
