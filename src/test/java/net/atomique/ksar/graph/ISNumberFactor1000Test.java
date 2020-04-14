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
import org.junit.runners.Parameterized.Parameters;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ISNumberFactor1000Test {

  private int factor;
  private NumberFormat sut;
  private DecimalFormat formatter;
  private StringBuilder expected;

  private double nr_to_test;
  private double expectedResult;
  private String expectedUnit;


  @Before
  public void setUp() throws Exception {

    factor = 1000;
    sut = new ISNumber(factor);

    formatter = new DecimalFormat("#,##0.0");

    expected = new StringBuilder();
  }

  /*    @After
    public void tearDown() throws Exception {

    }*/

  public ISNumberFactor1000Test(Double testNumber, Double expectedResult, String expectedUnit) {
    this.nr_to_test = testNumber;
    this.expectedResult = expectedResult;
    this.expectedUnit = expectedUnit;
  }

  @Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    return Arrays.asList(new Object[][]{
        {791.5, 791.5, "K"},     //K_to_K
        {9462.04, 9.5, "M"},     //K_to_1digit_M
        {25414.88, 25.4, "M"},     //K_to_2digit_M
        {725414.88, 725.4, "M"},     //K_to_3digit_M
        {2725414.88, 2.7, "G"},     //K_to_1digit_G
        {27254140.88, 27.3, "G"},     //K_to_2digit_G
        {272541400.88, 272.5, "G"},     //K_to_3digit_G
        //  {   2725414000.88,   2.7, "T"},     //K_to_1digit_T
        //  {  27254140000.88,  27.3, "T"},     //K_to_2digit_T
        //  { 272541400000.88, 272.5, "T"},     //K_to_3digit_T
    });
  }

  @Test
  public void testISNumber() {
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
