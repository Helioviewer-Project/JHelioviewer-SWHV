package org.helioviewer.jhv.plugins.swek.view;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.helioviewer.jhv.data.event.SWEKEventType;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
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
        SmallPanel filterPanel = new SmallPanel();
        filterPanel.setLayout(new GridLayout(filterPanels.size() + 1, 1));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(Color.white);
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
        filterPanel.setSmall();

        setContentPane(filterPanel);
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
