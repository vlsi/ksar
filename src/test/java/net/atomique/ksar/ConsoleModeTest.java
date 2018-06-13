/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class ConsoleModeTest {

  private String inputfile;
  private String outPDF;
  private String outIMG;
  private String outCSV;
  private String outFileCheck;

  private kSar sut;

  public ConsoleModeTest(String inputfile, String outPDF, String outIMG, String outCSV, String outFileCheck) {
    this.inputfile = inputfile;
    this.outPDF = outPDF;
    this.outIMG = outIMG;
    this.outCSV = outCSV;
    this.outFileCheck = outFileCheck;
  }

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void testConsole() {
    File f = new File(outFileCheck);
    f.delete();

    List<String> ars = new ArrayList<String>();

    ars.add("-n");
    ars.add("-h");
    ars.add("-input");
    ars.add(inputfile);

    if (!outPDF.equals("")) {
      ars.add("-outputPDF");
      ars.add(outPDF);
    }

    if (!outCSV.equals("")) {
      ars.add("-outputCSV");
      ars.add(outCSV);
    }

    if (!outIMG.equals("")) {
      ars.add("-outputIMG");
      ars.add(outIMG);
    }

    String[] args = new String[ars.size()];
    args = ars.toArray(args);

    // System.out.println(inputfile);
    Main.main(args);
    assertEquals(true, f.exists());
    f.delete();
  }

  @Parameterized.Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    return Arrays.asList(new Object[][] {
        { "src/test/resources/sar-10.1.5", "", "", "build/tmp/output_junit.csv", "build/tmp/output_junit.csv" },
        { "src/test/resources/sar-10.1.5", "", "build/tmp/output_junit_IMG_", "","build/tmp/output_junit_IMG_CPU all.png" },
        { "src/test/resources/sar-10.1.5", "build/tmp/output_junit.pdf", "", "", "build/tmp/output_junit.pdf" }// input                                                                                                       // outcsv
    });
  }
}

/*
 * Copyright 2018 The kSAR Project. All rights reserved. See the LICENSE file in
 * the project root for more information.
 */
