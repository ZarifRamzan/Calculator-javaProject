package com.mycompany.calculator;

import java.util.Objects;

/**
 * UI-independent implementation of the calculator arithmetic.
 *
 * <p>The class has no Swing dependency, so the calculation rules can be verified by unit tests and
 * reused by other front ends.
 */
public final class CalculatorEngine {

    /** Whole numbers below this limit can be rendered without an exponent. */
    private static final double WHOLE_NUMBER_LIMIT = 1e15d;

    private CalculatorEngine() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    /** Arithmetic operation supported by the calculator. */
    public enum Operator {

        /** Addition. */
        ADD(1),

        /** Subtraction. */
        SUBTRACT(2),

        /** Multiplication. */
        MULTIPLY(3),

        /** Division. */
        DIVIDE(4);

        private final int code;

        Operator(int code) {
            this.code = code;
        }

        /**
         * Resolves an operator from the numeric code used by the Swing UI.
         *
         * @param code operator code (1 = add, 2 = subtract, 3 = multiply, 4 = divide)
         * @return the matching operator
         * @throws IllegalArgumentException if the code is not supported
         */
        public static Operator fromCode(int code) {
            for (Operator operator : Operator.values()) {
                if (operator.code == code) {
                    return operator;
                }
            }
            throw new IllegalArgumentException("Unsupported operator code: " + code);
        }

        /**
         * Returns the numeric code of this operator.
         *
         * @return numeric operator code
         */
        public int getCode() {
            return code;
        }
    }

    /**
     * Applies an operation to two operands.
     *
     * @param operator operation to perform
     * @param left left-hand operand
     * @param right right-hand operand
     * @return result of the operation
     * @throws ArithmeticException if a division by zero is requested
     * @throws NullPointerException if {@code operator} is {@code null}
     */
    public static double calculate(Operator operator, double left, double right) {
        Objects.requireNonNull(operator, "operator must not be null");
        switch (operator) {
            case ADD:
                return left + right;
            case SUBTRACT:
                return left - right;
            case MULTIPLY:
                return left * right;
            case DIVIDE:
                return divide(left, right);
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    /**
     * Divides {@code dividend} by {@code divisor}.
     *
     * @param dividend value to be divided
     * @param divisor value to divide by
     * @return quotient of the division
     * @throws ArithmeticException if {@code divisor} is zero
     */
    public static double divide(double dividend, double divisor) {
        if (divisor == 0.0d) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return dividend / divisor;
    }

    /**
     * Converts a value to its percentage representation, for example {@code 25} becomes {@code 0.25}.
     *
     * @param value value to convert
     * @return the value divided by one hundred
     */
    public static double percentage(double value) {
        return value / 100.0d;
    }

    /**
     * Formats a value for display, dropping the trailing {@code .0} of whole numbers.
     *
     * @param value value to format
     * @return compact string representation of the value
     */
    public static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < WHOLE_NUMBER_LIMIT) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
