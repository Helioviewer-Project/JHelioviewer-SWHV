package org.helioviewer.jhv.data.gui.filter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.data.event.SWEKOperand;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.data.event.SWEKSupplier;

@SuppressWarnings("serial")
class FilterPanel extends JPanel {

    private final JLabel label;
    private final JSpinner spinner;

    private boolean enabled = false;

    private final SWEKParameter parameter;
    private final SWEKSupplier supplier;

    private final FilterDialog filterDialog;
    private final SWEKOperand operand;

    public FilterPanel(SWEKSupplier _supplier, SWEKParameter _parameter, JSpinner _spinner, FilterDialog _filterDialog, SWEKOperand _operand) {
        operand = _operand;
        filterDialog = _filterDialog;
        spinner = _spinner;
        parameter = _parameter;
        supplier = _supplier;

        JCheckBox enableButton = new JCheckBox();
        enableButton.addActionListener(e -> toggleEnabled());

        label = new JLabel(parameter.getParameterDisplayName() + ' ' + operand.representation);

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

    public void removeFilter() {
        FilterManager.removeFilters(supplier);
    }

    public void fireFilter() {
        FilterManager.fireFilters(supplier);
    }

    public void addFilter() {
        if (enabled) {
            Object oval = spinner.getValue();
            String pval = oval instanceof String ? String.valueOf(GOESLevel.getFloatValue((String) oval)) : String.valueOf(oval);
            SWEKParam param = new SWEKParam(parameter.getParameterName(), pval, operand);
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
