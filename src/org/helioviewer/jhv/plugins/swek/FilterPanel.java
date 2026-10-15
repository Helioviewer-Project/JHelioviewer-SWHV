package org.helioviewer.jhv.plugins.swek;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.event.GOESLevel;
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

    FilterPanel(SWEK.Parameter _parameter, JHVSpinner _spinner, FilterDialog _filterDialog, SWEK.Operand _operand) {
        operand = _operand;
        filterDialog = _filterDialog;
        spinner = _spinner;
        parameter = _parameter;

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
