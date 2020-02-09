package org.helioviewer.jhv.events.filter;

import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.gui.ComponentUtils;

@SuppressWarnings("serial")
public class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final JButton applyButton = new JButton("Apply");

    public FilterDialog(SWEKGroup group, SWEKSupplier supplier) {
        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        List<FilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(group, supplier, this, false);
        JPanel filterPanel = new JPanel(new GridLayout(filterPanels.size() + 1, 1));
        filterPanels.forEach(filterPanel::add);

        applyButton.addActionListener(e -> {
            FilterManager.removeFilters(supplier);
            filterPanels.forEach(FilterPanel::addFilter);

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
