/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.graph;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.DecimalFormat;
import java.util.stream.Stream;

public class ISNumberTest {
  public static Stream<Arguments> testValues() {
    DecimalFormat fmt = new DecimalFormat("#,##0.0");

    return Stream.of(
        arguments(791.5, fmt.format(791.5), fmt.format(791.5) + " K"),
        arguments(9462.04, fmt.format(9.5) + " K", fmt.format(9.5) + " M"),
        arguments(25414.88, fmt.format(25.4) + " K", fmt.format(25.4) + " M"),
        arguments(725414.88, fmt.format(725.4) + " K", fmt.format(725.4) + " M"),
        arguments(2725414.88, fmt.format(2.7) + " M", fmt.format(2.7) + " G"),
        arguments(27254140.88, fmt.format(27.3) + " M", fmt.format(27.3) + " G"),
        arguments(272541400.88, fmt.format(272.5) + " M", fmt.format(272.5) + " G"),
        arguments(2725414000.88, fmt.format(2.7) + " G", fmt.format(2725.4) + " G"),
        arguments(27254140000.88, fmt.format(27.3) + " G", fmt.format(27254.1) + " G"),
        arguments(272541400000.88, fmt.format(272.5) + " G", fmt.format(272541.4) + " G")
    );
  }

  @ParameterizedTest
  @MethodSource("testValues")
  public void testFormat(double testNumber, String expectedFactor1, String expectedFactor1000) {
    assertAll(
        () -> assertEquals(
            expectedFactor1,
            new ISNumber(1).format(testNumber),
            () -> "factor=1; input number: " + testNumber
        ),
        () -> assertEquals(
            expectedFactor1000,
            new ISNumber(1000).format(testNumber),
            () -> "factor=1000; input number: " + testNumber
        )
    );
  }
}
