package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.helioviewer.jhv.base.conversion.GOESLevel;
import org.helioviewer.jhv.data.cache.SWEKOperand;
import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.data.event.SWEKParam;
import org.helioviewer.jhv.data.event.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.FilterManager;
import org.helioviewer.jhv.plugins.swek.view.FilterDialog;

@SuppressWarnings("serial")
public class FilterPanel extends JPanel {

    private final JLabel label;
    private final JSpinner spinner;

    private boolean enabled = false;

    private final SWEKParameter parameter;
    private final SWEKEventType eventType;

    private final FilterDialog filterDialog;
    private final SWEKOperand operand;

    public FilterPanel(SWEKEventType _eventType, SWEKParameter _parameter, JSpinner _spinner, FilterDialog _filterDialog, SWEKOperand _operand) {
        operand = _operand;
        filterDialog = _filterDialog;
        spinner = _spinner;
        parameter = _parameter;
        eventType = _eventType;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.white);

        JCheckBox enableButton = new JCheckBox();
        enableButton.addActionListener(e -> toggleEnabled());

        label = new JLabel(parameter.getParameterDisplayName() + ' ' + operand.getStringRepresentation());

        spinner.setEnabled(enabled);
        label.setEnabled(enabled);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        p.add(enableButton, c);
        c.gridx = 1;
        p.add(label, c);
        c.gridx = 2;
        p.add(spinner, c);

        add(p, BorderLayout.CENTER);
    }

    public void removeFilter() {
        FilterManager.removeFilters(eventType);
    }

    public void fireFilter() {
        FilterManager.fireFilters(eventType);
    }

    public void addFilter() {
        if (enabled) {
            Object oval = spinner.getValue();
            String pval = oval instanceof String ? String.valueOf(GOESLevel.getFloatValue((String) oval)) : String.valueOf(oval);
            SWEKParam param = new SWEKParam(parameter.getParameterName(), pval, operand);
            FilterManager.addFilter(eventType, parameter, param);
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        label.setEnabled(enabled);
        spinner.setEnabled(enabled);
        filterDialog.filterParameterChanged();
    }

}
