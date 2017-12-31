package org.helioviewer.jhv.data.gui.filter;

import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.gui.ComponentUtils;

@SuppressWarnings("serial")
public class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final JButton applyButton = new JButton("Apply");

    public FilterDialog(SWEKSupplier supplier) {
        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        List<FilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(supplier, this, false);
        JPanel filterPanel = new JPanel(new GridLayout(filterPanels.size() + 1, 1));
        for (FilterPanel afp : filterPanels) {
            filterPanel.add(afp);
        }

        applyButton.addActionListener(e -> {
            FilterManager.removeFilters(supplier);
            for (FilterPanel afp : filterPanels) {
                afp.addFilter();
            }
            FilterManager.fireFilters(supplier);
            applyButton.setEnabled(false);
        });
        filterPanel.add(applyButton);

        setContentPane(filterPanel);
        ComponentUtils.smallVariant(this);
        pack();
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        setVisible(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        setVisible(false);
    }

    void filterParameterChanged() {
        applyButton.setEnabled(true);
    }

}
