package com.mycompany.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link CalculatorModel}. */
class CalculatorModelTest {

    private final CalculatorModel model = new CalculatorModel();

    @Test
    @DisplayName("starts with a zero display")
    void startsWithZeroDisplay() {
        assertEquals("0", model.getDisplay());
        assertFalse(model.isError());
        assertFalse(model.hasPendingOperator());
    }

    @Test
    @DisplayName("appends digits")
    void appendsDigits() {
        model.digit(1);
        model.digit(2);
        model.digit(3);
        assertEquals("123", model.getDisplay());
    }

    @Test
    @DisplayName("replaces a leading zero")
    void replacesLeadingZero() {
        model.digit(0);
        model.digit(5);
        assertEquals("5", model.getDisplay());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 10})
    @DisplayName("rejects digits outside the 0-9 range")
    void rejectsInvalidDigits(int digit) {
        assertThrows(IllegalArgumentException.class, () -> model.digit(digit));
    }

    @Test
    @DisplayName("limits the entry length")
    void limitsEntryLength() {
        for (int i = 0; i < 40; i++) {
            model.digit(9);
        }
        assertEquals(16, model.getDisplay().length());
    }

    @Test
    @DisplayName("adds a decimal separator only once")
    void addsDecimalSeparatorOnce() {
        model.digit(1);
        model.decimalSeparator();
        model.digit(5);
        model.decimalSeparator();
        assertEquals("1.5", model.getDisplay());
    }

    @Test
    @DisplayName("starts a fraction from the initial state")
    void startsFractionFromInitialState() {
        model.decimalSeparator();
        assertEquals("0.", model.getDisplay());
    }

    @Test
    @DisplayName("starts a fresh entry after an operator")
    void startsFreshEntryAfterOperator() {
        model.digit(7);
        model.operator(CalculatorEngine.Operator.ADD);
        model.decimalSeparator();
        assertEquals("0.", model.getDisplay());
    }

    @Test
    @DisplayName("removes the last character on backspace")
    void removesLastCharacterOnBackspace() {
        model.digit(1);
        model.digit(2);
        model.digit(3);
        model.backspace();
        assertEquals("12", model.getDisplay());
    }

    @Test
    @DisplayName("keeps a single zero on backspace")
    void keepsSingleZeroOnBackspace() {
        model.digit(7);
        model.backspace();
        assertEquals("0", model.getDisplay());
    }

    @Test
    @DisplayName("restores the decimal flag after backspace")
    void restoresDecimalFlagAfterBackspace() {
        model.digit(1);
        model.decimalSeparator();
        model.digit(5);
        model.backspace();
        model.digit(7);
        assertEquals("1.7", model.getDisplay());
    }

    @Test
    @DisplayName("ignores backspace right after an operator")
    void ignoresBackspaceRightAfterOperator() {
        model.digit(8);
        model.operator(CalculatorEngine.Operator.ADD);
        model.backspace();
        assertEquals("8", model.getDisplay());
    }

    @Test
    @DisplayName("resets the calculator on clear")
    void resetsCalculatorOnClear() {
        model.digit(9);
        model.operator(CalculatorEngine.Operator.ADD);
        model.clear();
        assertEquals("0", model.getDisplay());
        assertFalse(model.hasPendingOperator());
    }

    @ParameterizedTest
    @CsvSource({"ADD, 2, 3, 5", "SUBTRACT, 9, 4, 5", "MULTIPLY, 6, 7, 42", "DIVIDE, 9, 2, 4.5"})
    @DisplayName("evaluates a single operation")
    void evaluatesSingleOperation(String name, int left, int right, String expected) {
        CalculatorEngine.Operator operator = CalculatorEngine.Operator.valueOf(name);
        enter(left);
        model.operator(operator);
        enter(right);
        model.evaluate();
        assertEquals(expected, model.getDisplay());
        assertFalse(model.hasPendingOperator());
    }

    @Test
    @DisplayName("chains consecutive operations")
    void chainsConsecutiveOperations() {
        model.digit(2);
        model.operator(CalculatorEngine.Operator.ADD);
        model.digit(3);
        model.operator(CalculatorEngine.Operator.ADD);
        assertEquals("5", model.getDisplay());
        model.digit(4);
        model.evaluate();
        assertEquals("9", model.getDisplay());
    }

    @Test
    @DisplayName("replaces the pending operator")
    void replacesPendingOperator() {
        model.digit(8);
        model.operator(CalculatorEngine.Operator.ADD);
        model.operator(CalculatorEngine.Operator.SUBTRACT);
        model.digit(3);
        model.evaluate();
        assertEquals("5", model.getDisplay());
    }

    @Test
    @DisplayName("reports a division by zero")
    void reportsDivisionByZero() {
        divideByZero();
        assertTrue(model.isError());
        assertEquals(CalculatorModel.ERROR_TEXT, model.getDisplay());
    }

    @Test
    @DisplayName("recovers from an error on the next digit")
    void recoversFromErrorOnNextDigit() {
        divideByZero();
        model.digit(4);
        assertFalse(model.isError());
        assertEquals("4", model.getDisplay());
    }

    @Test
    @DisplayName("ignores operators while in the error state")
    void ignoresOperatorsInErrorState() {
        divideByZero();
        model.operator(CalculatorEngine.Operator.ADD);
        assertEquals(CalculatorModel.ERROR_TEXT, model.getDisplay());
    }

    @Test
    @DisplayName("ignores evaluate while in the error state")
    void ignoresEvaluateInErrorState() {
        divideByZero();
        model.evaluate();
        assertEquals(CalculatorModel.ERROR_TEXT, model.getDisplay());
    }

    @Test
    @DisplayName("ignores percent while in the error state")
    void ignoresPercentInErrorState() {
        divideByZero();
        model.percent();
        assertTrue(model.isError());
    }

    @Test
    @DisplayName("clears the error state on backspace")
    void clearsErrorStateOnBackspace() {
        divideByZero();
        model.backspace();
        assertFalse(model.isError());
        assertEquals("0", model.getDisplay());
    }

    @Test
    @DisplayName("evaluates without a pending operator")
    void evaluatesWithoutPendingOperator() {
        model.digit(4);
        model.evaluate();
        assertEquals("4", model.getDisplay());
    }

    @Test
    @DisplayName("converts the entry to a percentage")
    void convertsEntryToPercentage() {
        model.digit(5);
        model.digit(0);
        model.percent();
        assertEquals("0.5", model.getDisplay());
    }

    @Test
    @DisplayName("rejects a null operator")
    void rejectsNullOperator() {
        CalculatorEngine.Operator missing = null;
        assertThrows(NullPointerException.class, () -> model.operator(missing));
    }

    private void divideByZero() {
        model.digit(5);
        model.operator(CalculatorEngine.Operator.DIVIDE);
        model.digit(0);
        model.evaluate();
    }

    private void enter(int number) {
        for (char digit : Integer.toString(number).toCharArray()) {
            model.digit(digit - '0');
        }
    }
}
