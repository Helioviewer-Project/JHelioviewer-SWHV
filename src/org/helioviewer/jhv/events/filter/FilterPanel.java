package org.helioviewer.jhv.events.filter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.events.SWEK;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.gui.components.base.JHVSpinner;

@SuppressWarnings("serial")
class FilterPanel extends JPanel {

    private final JLabel label;
    private final JHVSpinner spinner;

    private boolean enabled = false;

    private final SWEK.Parameter parameter;
    private final SWEKSupplier supplier;

    private final FilterDialog filterDialog;
    private final SWEK.Operand operand;

    FilterPanel(SWEKSupplier _supplier, SWEK.Parameter _parameter, JHVSpinner _spinner, FilterDialog _filterDialog, SWEK.Operand _operand, boolean _enabled) {
        operand = _operand;
        filterDialog = _filterDialog;
        spinner = _spinner;
        parameter = _parameter;
        supplier = _supplier;

        JCheckBox enableButton = new JCheckBox();
        enableButton.addActionListener(e -> toggleEnabled());
        label = new JLabel(parameter.displayName() + ' ' + operand.representation);
        spinner.setEnabled(enabled);
        label.setEnabled(enabled);

        if (_enabled)
            enableButton.doClick();

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

    void addFilter() {
        if (enabled) {
            Object oval = spinner.getValue();
            String pval = oval instanceof String ? String.valueOf(GOESLevel.getFloatValue((String) oval)) : String.valueOf(oval);
            SWEK.Param param = new SWEK.Param(parameter.name(), pval, operand);
            FilterManager.addFilter(supplier, parameter, param);
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        label.setEnabled(enabled);
        spinner.setEnabled(enabled);
        filterDialog.filterParameterChanged();
    }

}
