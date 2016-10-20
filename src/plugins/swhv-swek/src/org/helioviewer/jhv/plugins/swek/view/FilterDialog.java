package org.helioviewer.jhv.plugins.swek.view;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanel;
import org.helioviewer.jhv.plugins.swek.view.filter.FilterPanelFactory;

@SuppressWarnings("serial")
public class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final SWEKEventType eventType;
    private final JButton filterApplyButton = new JButton("Apply");

    public FilterDialog(SWEKEventType eventType) {
        this.eventType = eventType;

        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        final List<FilterPanel> filterPanels = FilterPanelFactory.createFilterPanel(eventType, this);
        SmallPanel filterPanel = new SmallPanel();
        filterPanel.setLayout(new GridLayout(filterPanels.size() + 1, 1));
        filterPanel.setOpaque(false);
        filterPanel.setBackground(Color.white);
        for (FilterPanel afp : filterPanels) {
            filterPanel.add(afp);
        }

        filterApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (FilterPanel afp : filterPanels) {
                    afp.remove_filter();
                }
                for (FilterPanel afp : filterPanels) {
                    afp.add_filter();
                }
                for (FilterPanel afp : filterPanels) {
                    afp.fireFilter();
                }
                filterApplyButton.setEnabled(false);
            }
        });

        filterPanel.add(filterApplyButton);
        filterPanel.setSmall();

        setContentPane(filterPanel);
        pack();
        validate();
    }

    @Override
    public void focusGained(FocusEvent arg0) {
    }

    @Override
    public void focusLost(FocusEvent arg0) {
        setVisible(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent arg0) {
    }

    @Override
    public void windowLostFocus(WindowEvent arg0) {
        setVisible(false);
    }

    public void filterParameterChanged() {
        filterApplyButton.setEnabled(true);
    }

}
