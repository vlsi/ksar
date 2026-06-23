/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class VersionNumber {

  private static final Logger log = LoggerFactory.getLogger(VersionNumber.class);

  static {

    StringBuilder tmpstr = new StringBuilder();

    InputStream is = VersionNumber.class.getClassLoader().getResourceAsStream("kSar.version");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

      String line;
      while ((line = reader.readLine()) != null) {
        tmpstr.append(line);
      }

      version_string = tmpstr.toString();

    } catch (IOException ex) {
      log.error("Unable to read ksar version", ex);
    }
  }

  public static String getVersionString() {
    return version_string;
  }

  private static String version_string;

}
