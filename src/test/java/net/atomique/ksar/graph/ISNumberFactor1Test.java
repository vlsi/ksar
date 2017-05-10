package net.atomique.ksar.graph;

//import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ISNumberFactor1Test {

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
        sut = new ISNumber(factor);

        formatter = new DecimalFormat("#,##0.0");

        expected = new StringBuilder();
    }

/*    @After
    public void tearDown() throws Exception {

    }*/

    public ISNumberFactor1Test(Double testNumber, Double expectedResult, String expectedUnit) {
        this.nr_to_test = testNumber;
        this.expectedResult = expectedResult;
        this.expectedUnit = expectedUnit;
    }

    @Parameters
    public static Collection testValues() {

        // current Byte, expected value, expected unit, test name
        return Arrays.asList(new Object[][] {
                {           791.5, 791.5, ""},      //x_to_K
                {         9462.04,   9.5, "K"},     //x_to_1digit_K
                {        25414.88,  25.4, "K"},     //x_to_2digit_K
                {       725414.88, 725.4, "K"},     //x_to_3digit_K
                {      2725414.88,   2.7, "M"},     //x_to_1digit_M
                {     27254140.88,  27.3, "M"},     //x_to_2digit_M
                {    272541400.88, 272.5, "M"},     //x_to_3digit_M
                {   2725414000.88,   2.7, "G"},     //x_to_1digit_G
                {  27254140000.88,  27.3, "G"},     //x_to_2digit_G
                { 272541400000.88, 272.5, "G"},     //x_to_3digit_G
        });
    }

    @Test
    public void testISNumber() {
        //System.out.println("Parameterized Number is : " + nr_to_test);

        expected.append(formatter.format( expectedResult ));
        if ( ! expectedUnit.isEmpty() ){
            expected.append(" ");
            expected.append(expectedUnit);
        }

        assertEquals(expected.toString() , sut.format(nr_to_test));
    }
/*    @Test
    public void format() throws Exception {

    }*/

 /*   @Test
    public void parse() throws Exception {

    }
*/
}