package com.mycompany.calculator;

import java.util.Objects;

/**
 * State machine behind the calculator user interface.
 *
 * <p>It owns the text shown on the display, the pending operation and the numeric entry buffer.
 * All arithmetic is delegated to {@link CalculatorEngine}, which keeps this class free of Swing
 * dependencies and therefore fully unit testable.
 */
public final class CalculatorModel {

    /** Text shown when an operation cannot be completed. */
    static final String ERROR_TEXT = "Error";

    /** Maximum number of characters accepted in the entry buffer. */
    private static final int MAX_ENTRY_LENGTH = 16;

    private String entry = "0";
    private boolean replaceEntry = true;
    private boolean decimalPoint;
    private boolean error;
    private Double accumulator;
    private CalculatorEngine.Operator pendingOperator;

    /**
     * Returns the text that should be shown on the display.
     *
     * @return current display text
     */
    public String getDisplay() {
        return entry;
    }

    /**
     * Tells whether the last operation failed.
     *
     * @return {@code true} when the calculator is in the error state
     */
    public boolean isError() {
        return error;
    }

    /**
     * Tells whether an operator has been entered and still awaits its right-hand operand.
     *
     * @return {@code true} when an operation is pending
     */
    public boolean hasPendingOperator() {
        return pendingOperator != null;
    }

    /**
     * Appends a digit to the current entry.
     *
     * @param digit digit between {@code 0} and {@code 9}
     * @throws IllegalArgumentException if the digit is outside that range
     */
    public void digit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Digit must be between 0 and 9: " + digit);
        }
        resetError();
        String value = Integer.toString(digit);
        if (replaceEntry || entry.equals("0")) {
            entry = value;
            replaceEntry = false;
            decimalPoint = false;
            return;
        }
        if (entry.length() < MAX_ENTRY_LENGTH) {
            entry = entry + value;
        }
    }

    /**
     * Starts or extends the fractional part of the current entry.
     */
    public void decimalSeparator() {
        resetError();
        if (replaceEntry) {
            entry = "0.";
            replaceEntry = false;
            decimalPoint = true;
            return;
        }
        if (!decimalPoint && entry.length() < MAX_ENTRY_LENGTH) {
            entry = entry + ".";
            decimalPoint = true;
        }
    }

    /**
     * Resets the calculator, including any pending operation.
     */
    public void clear() {
        entry = "0";
        replaceEntry = true;
        decimalPoint = false;
        error = false;
        accumulator = null;
        pendingOperator = null;
    }

    /**
     * Removes the last character of the current entry.
     */
    public void backspace() {
        if (error) {
            clear();
            return;
        }
        if (replaceEntry) {
            return;
        }
        if (entry.length() <= 1) {
            entry = "0";
        } else {
            entry = entry.substring(0, entry.length() - 1);
        }
        decimalPoint = entry.contains(".");
    }

    /**
     * Records an operator, evaluating any operation that is already pending.
     *
     * @param operator operator pressed by the user
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public void operator(CalculatorEngine.Operator operator) {
        Objects.requireNonNull(operator, "operator must not be null");
        if (error) {
            return;
        }
        double current = currentValue();
        if (pendingOperator != null && !replaceEntry) {
            accumulator = applyPending(current);
            if (error) {
                return;
            }
            entry = CalculatorEngine.format(accumulator);
        } else if (accumulator == null) {
            accumulator = current;
        }
        pendingOperator = operator;
        replaceEntry = true;
        decimalPoint = false;
    }

    /**
     * Evaluates the pending operation, if any.
     */
    public void evaluate() {
        if (error) {
            return;
        }
        double current = currentValue();
        if (pendingOperator == null) {
            entry = CalculatorEngine.format(current);
            replaceEntry = true;
            decimalPoint = entry.contains(".");
            return;
        }
        double result = applyPending(current);
        if (error) {
            return;
        }
        entry = CalculatorEngine.format(result);
        accumulator = null;
        pendingOperator = null;
        replaceEntry = true;
        decimalPoint = entry.contains(".");
    }

    /**
     * Converts the current entry to its percentage, for example {@code 50} becomes {@code 0.5}.
     */
    public void percent() {
        if (error) {
            return;
        }
        entry = CalculatorEngine.format(CalculatorEngine.percentage(currentValue()));
        replaceEntry = true;
        decimalPoint = entry.contains(".");
    }

    private void resetError() {
        if (error) {
            clear();
        }
    }

    private double currentValue() {
        return Double.parseDouble(entry);
    }

    private double applyPending(double operand) {
        double left = accumulator == null ? 0.0d : accumulator;
        try {
            return CalculatorEngine.calculate(pendingOperator, left, operand);
        } catch (ArithmeticException ex) {
            error = true;
            entry = ERROR_TEXT;
            accumulator = null;
            pendingOperator = null;
            replaceEntry = true;
            decimalPoint = false;
            return 0.0d;
        }
    }
}
