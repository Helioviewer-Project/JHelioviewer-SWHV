package org.helioviewer.jhv.plugins.swek.view;

import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanel;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanelFactory;

@SuppressWarnings("serial")
public class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final JButton applyButton = new JButton("Apply");

    public FilterDialog(SWEKEventType eventType) {
        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        List<FilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(eventType, this);
        JPanel filterPanel = new JPanel(new GridLayout(filterPanels.size() + 1, 1));
        for (FilterPanel afp : filterPanels) {
            filterPanel.add(afp);
        }

        applyButton.addActionListener(e -> {
            for (FilterPanel afp : filterPanels) {
                afp.removeFilter();
            }
            for (FilterPanel afp : filterPanels) {
                afp.addFilter();
            }
            for (FilterPanel afp : filterPanels) {
                afp.fireFilter();
            }
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

    public void filterParameterChanged() {
        applyButton.setEnabled(true);
    }

}
