/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import picocli.CommandLine.Option;

public class CmdLineArgs {

  @Option(names = "-input", description = "sar file to be processed")
  private String filename;

  @Option(names = "-help", description = "show usage", help=true)
  private boolean help;

  @Option(names = "-version", description = "print version information")
  private boolean version = false;

  @Option(names = "-test", description = "debug mode")
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
