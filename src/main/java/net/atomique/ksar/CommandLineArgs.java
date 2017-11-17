/*
 * Copyright 2017 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import com.beust.jcommander.Parameter;

public class CommandLineArgs {

  @Parameter(names = "-input", description = "sar file to be processed")
  private String filename;

  @Parameter(names = "-help", description = "show usage", help=true)
  private boolean help;

  @Parameter(names = "-version", description = "print version information")
  private boolean version = false;

  @Parameter(names = "-test", description = "debug mode")
  private boolean debug = false;

  public String getFilename() {
    return filename;
  }

  public boolean isHelp() {
    return help;
  }

  public boolean isVersion() {
    return version;
  }

  public boolean isDebug() {
    return debug;
  }

}
