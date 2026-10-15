package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.math.MathContext;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.astronomy.GOESLevel;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.gui.component.JHVSpinner;

@SuppressWarnings("serial")
class FilterPanel extends JPanel {

    private final JLabel label;
    private final JHVSpinner spinner;

    private boolean enabled = false;

    private final SWEK.Parameter parameter;

    private final FilterDialog filterDialog;
    private final SWEK.Operand operand;

    FilterPanel(SWEK.Parameter _parameter, SWEK.Operand _operand, FilterDialog _filterDialog) {
        operand = _operand;
        filterDialog = _filterDialog;
        parameter = _parameter;
        spinner = createSpinner(parameter.filter());
        spinner.addChangeListener(e -> filterDialog.filterParameterChanged());

        JCheckBox enableButton = new JCheckBox();
        enableButton.addActionListener(e -> toggleEnabled());
        label = new JLabel(parameter.displayName() + ' ' + operand.representation);
        spinner.setEnabled(enabled);
        label.setEnabled(enabled);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        add(enableButton, c);
        c.gridx = 1;
        add(label, c);
        c.gridx = 2;
        add(spinner, c);
    }

    private static JHVSpinner createSpinner(SWEK.ParameterFilter filter) {
        if (filter.isFlare())
            return new JHVSpinner(new FlareSpinnerModel(filter.startValue(), filter.min(), filter.max(), filter.stepSize()));

        JHVSpinner spinner = new JHVSpinner(filter.startValue(), filter.min(), filter.max(), filter.stepSize());
        spinner.setEditor(new JHVSpinner.NumberEditor(spinner, getSpinnerFormat(filter.min(), filter.max())));
        return spinner;
    }

    private static String getSpinnerFormat(double minimumValue, double maximumValue) {
        StringBuilder spinnerFormat = new StringBuilder("0");
        String minString = new BigDecimal(minimumValue, new MathContext(12)).stripTrailingZeros().toPlainString();
        String maxString = new BigDecimal(maximumValue, new MathContext(12)).stripTrailingZeros().toPlainString();
        int integerPlacesMin = minString.indexOf('.');
        int decimalPlacesMin = minString.length() - integerPlacesMin - 1;
        int integerPlacesMax = maxString.indexOf('.');
        int decimalPlacesMax = maxString.length() - integerPlacesMax - 1;
        if (integerPlacesMax != -1 && integerPlacesMin != -1) {
            spinnerFormat.append('.');
            spinnerFormat.repeat("0", Math.max(0, Math.max(decimalPlacesMax, decimalPlacesMin)));
        }
        return spinnerFormat.toString();
    }

    SWEK.Param filter() {
        if (enabled) {
            Object oval = spinner.getValue();
            double value = oval instanceof String str ? GOESLevel.getFloatValue(str) : ((Number) oval).doubleValue();
            return new SWEK.Param(parameter.name(), value, operand);
        }
        return null;
    }

    private void toggleEnabled() {
        enabled = !enabled;
        label.setEnabled(enabled);
        spinner.setEnabled(enabled);
        filterDialog.filterParameterChanged();
    }

}
