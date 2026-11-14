package com.mycompany.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for {@link CalculatorEngine}. */
class CalculatorEngineTest {

    @ParameterizedTest(name = "{1} {2} {3} = {4}")
    @CsvSource({
        "ADD, 2.0, 3.0, 5.0",
        "SUBTRACT, 10.0, 4.0, 6.0",
        "MULTIPLY, 6.0, 7.0, 42.0",
        "DIVIDE, 9.0, 2.0, 4.5"
    })
    @DisplayName("calculate applies the requested operator")
    void calculateAppliesTheRequestedOperator(String name, double left, double right, double expected) {
        CalculatorEngine.Operator operator = CalculatorEngine.Operator.valueOf(name);
        assertEquals(expected, CalculatorEngine.calculate(operator, left, right), 1e-9d);
    }

    @Test
    @DisplayName("calculate rejects a division by zero")
    void calculateRejectsDivisionByZero() {
        CalculatorEngine.Operator divide = CalculatorEngine.Operator.DIVIDE;
        ArithmeticException error = assertThrows(
                ArithmeticException.class, () -> CalculatorEngine.calculate(divide, 1.0d, 0.0d));
        assertEquals("Division by zero is not allowed", error.getMessage());
    }

    @Test
    @DisplayName("calculate rejects a null operator")
    void calculateRejectsNullOperator() {
        CalculatorEngine.Operator missing = null;
        assertThrows(NullPointerException.class, () -> CalculatorEngine.calculate(missing, 1.0d, 2.0d));
    }

    @ParameterizedTest(name = "code {0} maps to {1}")
    @CsvSource({"1, ADD", "2, SUBTRACT", "3, MULTIPLY", "4, DIVIDE"})
    @DisplayName("fromCode resolves the legacy numeric codes")
    void fromCodeResolvesLegacyCodes(int code, String name) {
        assertEquals(CalculatorEngine.Operator.valueOf(name), CalculatorEngine.Operator.fromCode(code));
    }

    @Test
    @DisplayName("fromCode rejects unknown codes")
    void fromCodeRejectsUnknownCodes() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CalculatorEngine.Operator.fromCode(99));
        assertTrue(error.getMessage().contains("99"));
    }

    @ParameterizedTest(name = "{0} has code {1}")
    @CsvSource({"ADD, 1", "SUBTRACT, 2", "MULTIPLY, 3", "DIVIDE, 4"})
    @DisplayName("getCode exposes the numeric code")
    void getCodeExposesNumericCode(String name, int expected) {
        assertEquals(expected, CalculatorEngine.Operator.valueOf(name).getCode());
    }

    @Test
    @DisplayName("divide performs a floating point division")
    void dividePerformsFloatingPointDivision() {
        assertEquals(4.0d, CalculatorEngine.divide(8.0d, 2.0d), 1e-9d);
    }

    @Test
    @DisplayName("divide rejects a zero divisor")
    void divideRejectsZeroDivisor() {
        assertThrows(ArithmeticException.class, () -> CalculatorEngine.divide(1.0d, 0.0d));
    }

    @ParameterizedTest(name = "{0}% is {1}")
    @CsvSource({"100.0, 1.0", "25.0, 0.25", "0.0, 0.0", "-50.0, -0.5"})
    @DisplayName("percentage converts a value to a fraction")
    void percentageConvertsToFraction(double value, double expected) {
        assertEquals(expected, CalculatorEngine.percentage(value), 1e-9d);
    }

    @Test
    @DisplayName("format drops the decimal part of whole numbers")
    void formatDropsDecimalPartOfWholeNumbers() {
        assertEquals("7", CalculatorEngine.format(7.0d));
        assertEquals("-7", CalculatorEngine.format(-7.0d));
        assertEquals("0", CalculatorEngine.format(0.0d));
    }

    @Test
    @DisplayName("format keeps the fraction of decimal numbers")
    void formatKeepsFractionOfDecimalNumbers() {
        assertEquals("7.5", CalculatorEngine.format(7.5d));
    }

    @Test
    @DisplayName("format falls back to scientific notation for large values")
    void formatFallsBackToScientificNotationForLargeValues() {
        assertEquals("1.0E20", CalculatorEngine.format(1.0e20d));
    }

    @Test
    @DisplayName("format renders special values")
    void formatRendersSpecialValues() {
        assertEquals("NaN", CalculatorEngine.format(Double.NaN));
        assertEquals("Infinity", CalculatorEngine.format(Double.POSITIVE_INFINITY));
    }
}
