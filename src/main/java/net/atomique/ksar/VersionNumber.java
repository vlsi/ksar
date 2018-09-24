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

public class VersionNumber {

  private static final Logger log = LoggerFactory.getLogger(VersionNumber.class);

  private VersionNumber() {
    StringBuilder tmpstr = new StringBuilder();
    BufferedReader reader = null;
    try {
      InputStream is = this.getClass().getResourceAsStream("/kSar.version");
      InputStreamReader isr = new InputStreamReader(is);
      reader = new BufferedReader(isr);
      String line = "";
      while ((line = reader.readLine()) != null) {
        tmpstr.append(line);
      }
      reader.close();
    } catch (IOException ex) {
      log.error("Unable to read Current version", ex);
      return;
    }
    setVersionNumber(tmpstr.toString());
  }

  private static void setVersionNumber(String version) {
    version_string = version;
  }

  public static String getVersionString() {
    return version_string;
  }

  private static String version_string;

}
