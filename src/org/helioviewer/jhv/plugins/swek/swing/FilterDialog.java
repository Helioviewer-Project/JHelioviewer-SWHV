package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.gui.MainFrame;

@SuppressWarnings("serial")
final class FilterDialog extends JDialog implements FocusListener, WindowFocusListener {

    private final JButton applyButton = new JButton("Apply");

    FilterDialog(SWEKSession session, SWEKSupplier supplier) {
        super(MainFrame.get());
        setUndecorated(true);
        addFocusListener(this);
        addWindowFocusListener(this);

        List<FilterPanel> filterPanels = new ArrayList<>();
        for (SWEK.Parameter parameter : supplier.getParameterList()) {
            SWEK.ParameterFilter filter = parameter.filter();
            if (filter == null)
                continue;
            for (SWEK.Operand operand : filter.operands())
                filterPanels.add(new FilterPanel(parameter, operand, () -> applyButton.setEnabled(true)));
        }

        JPanel filterPanel = new JPanel(new GridLayout(filterPanels.size() + 1, 1));
        filterPanels.forEach(filterPanel::add);

        applyButton.addActionListener(e -> {
            List<SWEK.Param> selected = new ArrayList<>();
            for (FilterPanel panel : filterPanels) {
                SWEK.Param value = panel.filter();
                if (value != null) selected.add(value);
            }
            session.setFilters(supplier, selected);
            applyButton.setEnabled(false);
        });
        filterPanel.add(applyButton);

        setContentPane(filterPanel);
        pack();
    }

    @Override
    public void focusGained(FocusEvent e) {}

    @Override
    public void focusLost(FocusEvent e) {
        setVisible(false);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {}

    @Override
    public void windowLostFocus(WindowEvent e) {
        setVisible(false);
    }

}
