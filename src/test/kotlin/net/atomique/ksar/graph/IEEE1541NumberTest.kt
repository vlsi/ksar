/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.DecimalFormat;
import java.util.stream.Stream;

public class IEEE1541NumberTest {
  public static Stream<Arguments> testValues() {
    DecimalFormat fmt = new DecimalFormat("#,##0.0");

    return Stream.of(
        arguments(791.5, fmt.format(791.5), fmt.format(791.5) + " Ki"),
        arguments(9462.04, fmt.format(9.2) + " Ki", fmt.format(9.2) + " Mi"),
        arguments(25414.88, fmt.format(24.8) + " Ki", fmt.format(24.8) + " Mi"),
        arguments(725414.88, fmt.format(708.4) + " Ki", fmt.format(708.4) + " Mi"),
        arguments(2725414.88, fmt.format(2.6) + " Mi", fmt.format(2.6) + " Gi"),
        arguments(27254140.88, fmt.format(26.0) + " Mi", fmt.format(26.0) + " Gi"),
        arguments(272541400.88, fmt.format(259.9) + " Mi", fmt.format(259.9) + " Gi"),
        arguments(2725414000.88, fmt.format(2.5) + " Gi", fmt.format(2599.2) + " Gi"),
        arguments(27254140000.88, fmt.format(25.4) + " Gi", fmt.format(25991.6) + " Gi"),
        arguments(272541400000.88, fmt.format(253.8) + " Gi", fmt.format(259915.7) + " Gi")
    );
  }

  @ParameterizedTest
  @MethodSource("testValues")
  public void testFormat(double testNumber, String expectedFactor1, String expectedFactor1024) {
    Assertions.assertAll(
        () -> assertEquals(
            expectedFactor1,
            new IEEE1541Number(1).format(testNumber),
            () -> "factor=1; input number: " + testNumber
        ),
        () -> assertEquals(
            expectedFactor1024,
            new IEEE1541Number(1024).format(testNumber),
            () -> "factor=1024; input number: " + testNumber
        )
    );
  }
}
