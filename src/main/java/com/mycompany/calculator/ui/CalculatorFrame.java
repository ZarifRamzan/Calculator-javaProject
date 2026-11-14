package com.mycompany.calculator.ui;

import com.mycompany.calculator.CalculatorEngine;
import com.mycompany.calculator.CalculatorModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Swing front end of the calculator.
 *
 * <p>The frame only translates user gestures into calls on {@link CalculatorModel}; every
 * calculation rule lives in the model and in {@link CalculatorEngine}, where it is unit tested.
 */
public final class CalculatorFrame extends JFrame {

    private static final Font DISPLAY_FONT = new Font(Font.MONOSPACED, Font.BOLD, 28);
    private static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    private static final int KEYPAD_GAP = 4;

    private final CalculatorModel model = new CalculatorModel();
    private final JTextField display = new JTextField();
    private final List<JButton> keypad = new ArrayList<>();
    private final JRadioButton onButton = new JRadioButton("On", true);
    private final JRadioButton offButton = new JRadioButton("Off");

    /**
     * Creates and lays out the calculator window.
     */
    public CalculatorFrame() {
        super("Calculator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(340, 470));
        setLayout(new BorderLayout(0, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildDisplay(), BorderLayout.NORTH);
        add(buildKeypad(), BorderLayout.CENTER);
        add(buildPowerPanel(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    private JTextField buildDisplay() {
        display.setEditable(false);
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(DISPLAY_FONT);
        display.setPreferredSize(new Dimension(0, 56));
        return display;
    }

    private JPanel buildKeypad() {
        JPanel panel = new JPanel(new BorderLayout(0, KEYPAD_GAP));

        JPanel grid = new JPanel(new GridLayout(4, 4, KEYPAD_GAP, KEYPAD_GAP));
        grid.add(button("C", this::clear));
        grid.add(button("<", this::backspace));
        grid.add(button("%", this::percent));
        grid.add(button("\u00f7", () -> operator(CalculatorEngine.Operator.DIVIDE)));
        grid.add(digitButton(7));
        grid.add(digitButton(8));
        grid.add(digitButton(9));
        grid.add(button("\u00d7", () -> operator(CalculatorEngine.Operator.MULTIPLY)));
        grid.add(digitButton(4));
        grid.add(digitButton(5));
        grid.add(digitButton(6));
        grid.add(button("-", () -> operator(CalculatorEngine.Operator.SUBTRACT)));
        grid.add(digitButton(1));
        grid.add(digitButton(2));
        grid.add(digitButton(3));
        grid.add(button("+", () -> operator(CalculatorEngine.Operator.ADD)));

        JPanel bottom = new JPanel(new GridLayout(1, 3, KEYPAD_GAP, KEYPAD_GAP));
        bottom.add(digitButton(0));
        bottom.add(button(".", this::decimalSeparator));
        bottom.add(button("=", this::evaluate));

        panel.add(grid, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPowerPanel() {
        ButtonGroup group = new ButtonGroup();
        group.add(onButton);
        group.add(offButton);
        onButton.addActionListener(event -> setKeypadEnabled(true));
        offButton.addActionListener(event -> setKeypadEnabled(false));

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(onButton);
        panel.add(offButton);
        return panel;
    }

    private JButton digitButton(int digit) {
        return button(Integer.toString(digit), () -> {
            model.digit(digit);
            refresh();
        });
    }

    private JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.setFont(BUTTON_FONT);
        button.addActionListener(event -> action.run());
        keypad.add(button);
        return button;
    }

    private void operator(CalculatorEngine.Operator operator) {
        model.operator(operator);
        refresh();
    }

    private void decimalSeparator() {
        model.decimalSeparator();
        refresh();
    }

    private void clear() {
        model.clear();
        refresh();
    }

    private void backspace() {
        model.backspace();
        refresh();
    }

    private void percent() {
        model.percent();
        refresh();
    }

    private void evaluate() {
        model.evaluate();
        refresh();
    }

    private void refresh() {
        display.setText(model.getDisplay());
    }

    private void setKeypadEnabled(boolean enabled) {
        for (JButton button : keypad) {
            button.setEnabled(enabled);
        }
    }

    /**
     * Launches the calculator application.
     *
     * @param args command-line arguments, ignored
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Fall back to the default look and feel.
            }
            new CalculatorFrame().setVisible(true);
        });
    }
}
