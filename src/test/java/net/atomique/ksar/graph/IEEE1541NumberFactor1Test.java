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
public class IEEE1541NumberFactor1Test {

  private int factor;
  private NumberFormat sut;
  private DecimalFormat formatter;
  private StringBuilder expected;

  private double nr_to_test;
  private double expectedResult;
  private String expectedUnit;


  @Before
  public void setUp() throws Exception {

    factor = 1;
    sut = new IEEE1541Number(factor);

    formatter = new DecimalFormat("#,##0.0");

    expected = new StringBuilder();
  }

  /*    @After
    public void tearDown() throws Exception {

    }*/

  public IEEE1541NumberFactor1Test(Double testNumber, Double expectedResult, String expectedUnit) {
    this.nr_to_test = testNumber;
    this.expectedResult = expectedResult;
    this.expectedUnit = expectedUnit;
  }

  @Parameterized.Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    return Arrays.asList(new Object[][]{
        {791.5, 791.5, ""},       //x_to_Ki
        {9462.04, 9.2, "Ki"},     //x_to_1digit_Ki
        {25414.88, 24.8, "Ki"},     //x_to_2digit_Ki
        {725414.88, 708.4, "Ki"},     //x_to_3digit_Ki
        {2725414.88, 2.6, "Mi"},     //x_to_1digit_Mi
        {27254140.88, 26.0, "Mi"},     //x_to_2digit_Mi
        {272541400.88, 259.9, "Mi"},     //x_to_3digit_Mi
        {2725414000.88, 2.5, "Gi"},     //x_to_1digit_Gi
        {27254140000.88, 25.4, "Gi"},     //x_to_2digit_Gi
        {272541400000.88, 253.8, "Gi"},     //x_to_3digit_Gi
    });
  }

  @Test
  public void testIEEE1541Number() {
    //System.out.println("Parameterized Number is : " + nr_to_test);

    expected.append(formatter.format(expectedResult));
    if (!expectedUnit.isEmpty()) {
      expected.append(" ");
      expected.append(expectedUnit);
    }

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
