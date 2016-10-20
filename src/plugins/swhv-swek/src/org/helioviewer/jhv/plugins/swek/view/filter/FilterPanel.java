package org.helioviewer.jhv.plugins.swek.view.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import org.helioviewer.jhv.base.conversion.GOESLevelConversion;
import org.helioviewer.jhv.data.container.cache.SWEKOperand;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.plugins.swek.download.FilterManager;
import org.helioviewer.jhv.plugins.swek.view.FilterDialog;

@SuppressWarnings("serial")
public class FilterPanel extends JPanel {

    private JButton filterEnableButton;
    private boolean enabled = false;

    private final JSpinner spinner;
    private JLabel label;
    private final SWEKParameter parameter;
    private final SWEKEventType eventType;

    private final FilterManager filterManager;
    private final FilterDialog filterDialog;
    private final SWEKOperand operand;

    public FilterPanel(SWEKEventType _eventType, SWEKParameter _parameter, JSpinner _spinner, FilterDialog _filterDialog, SWEKOperand _operand) {
        operand = _operand;
        filterDialog = _filterDialog;
        spinner = _spinner;
        parameter = _parameter;
        eventType = _eventType;
        filterManager = FilterManager.getSingletonInstance();

        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(Color.white);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(initFilterComponents(), BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void remove_filter() {
        filterManager.removeFilters(eventType);
    }

    public void fireFilter() {
        filterManager.fireFilters(eventType);
    }

    public void add_filter() {
        if (enabled) {
            String pval;
            Object sval = spinner.getValue();
            if (sval instanceof String)
                pval = String.valueOf(GOESLevelConversion.getFloatValue(String.valueOf(spinner.getValue())));
            else
                pval = String.valueOf(spinner.getValue());
            SWEKParam param = new SWEKParam(parameter.getParameterName(), pval, operand);
            filterManager.addFilter(eventType, parameter, param);
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        spinner.setEnabled(enabled);
        label.setEnabled(enabled);
        if (!enabled)
            filterEnableButton.setText("Activate");
        else
            filterEnableButton.setText("Disable ");
        filterDialog.filterParameterChanged();
    }

    private void initEnableButton() {
        filterEnableButton = new JButton("Activate");
        filterEnableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleEnabled();
            }
        });
    }

    private JComponent initFilterComponents() {
        label = new JLabel(parameter.getParameterDisplayName() + " " + operand.getStringRepresentation());
        initEnableButton();
        spinner.setEnabled(enabled);
        label.setEnabled(enabled);
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.5;
        p.add(label, c);
        c.gridx = 1;
        p.add(spinner, c);
        c.gridx = 2;
        p.add(filterEnableButton, c);
        return p;
    }

}
