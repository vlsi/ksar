/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.graph;

//import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class IEEE1541NumberFactor1024Test {

  private int factor;
  private NumberFormat sut;
  private DecimalFormat formatter;
  private StringBuilder expected;

  private double nr_to_test;
  private double expectedResult;
  private String expectedUnit;


  @Before
  public void setUp() throws Exception {

    factor = 1024;
    sut = new IEEE1541Number(factor);

    formatter = new DecimalFormat("#,##0.0");

    expected = new StringBuilder();
  }

  /* @After
    public void tearDown() throws Exception {

    }*/

  public IEEE1541NumberFactor1024Test(Double testNumber, Double expectedResult,
      String expectedUnit) {
    this.nr_to_test = testNumber;
    this.expectedResult = expectedResult;
    this.expectedUnit = expectedUnit;
  }

  @Parameterized.Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    return Arrays.asList(new Object[][]{
        {791.5, 791.5, "Ki"},     //Ki_to_Mi
        {9462.0, 9.2, "Mi"},     //Ki_to_1digit_Mi
        {25414.8, 24.8, "Mi"},     //Ki_to_2digit_Mi
        {725414.8, 708.4, "Mi"},     //Ki_to_3digit_Mi
        {2725414.8, 2.6, "Gi"},     //Ki_to_1digit_Gi
        {27254140.8, 26.0, "Gi"},     //Ki_to_2digit_Gi
        {272541400.8, 259.9, "Gi"},     //Ki_to_3digit_Gi
    //   {    2725414000.8,   2.5, "TiB"},     //Ki_to_1digit_Ti
    //   {   27254140000.8,  25.4, "TiB"},     //Ki_to_2digit_Ti
    //   {  272541400000.8, 253.8, "TiB"},     //Ki_to_3digit_Ti
    });
  }

  @Test
  public void testIEEE1541Number() {
    //System.out.println("Parameterized Number is : " + nr_to_test);

    expected.append(formatter.format(expectedResult));
    expected.append(" ");
    expected.append(expectedUnit);

    assertEquals(expected.toString(), sut.format(nr_to_test));
  }

  /*    @Test
    public void format() throws Exception {

    }*/

  /*   @Test
    public void parse() throws Exception {

    }
*/
}
