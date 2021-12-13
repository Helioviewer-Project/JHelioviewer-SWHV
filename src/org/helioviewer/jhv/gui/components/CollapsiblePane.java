package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UIGlobals;

// This panel consists of a toggle button and one arbitrary component. Clicking
// the toggle button will toggle the visibility of the component.
@SuppressWarnings("serial")
public class CollapsiblePane extends JComponent implements ActionListener {

    final CollapsiblePaneButton toggleButton;
    private final JPanel component;
    private String title;

    public CollapsiblePane(String _title, Component managed, boolean startExpanded) {
        setLayout(new BorderLayout());

        component = new JPanel(new BorderLayout());
        component.add(managed);
        ComponentUtils.setVisible(component, startExpanded);

        toggleButton = new CollapsiblePaneButton();
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(UIGlobals.uiFontSmallBold);
        int height = toggleButton.getFontMetrics(UIGlobals.uiFontSmallBold).getHeight();
        toggleButton.setPreferredSize(new Dimension(-1, height + 4));
        toggleButton.addActionListener(this);

        setTitle(_title);

        add(toggleButton, BorderLayout.PAGE_START);
        add(component, BorderLayout.CENTER);
    }

    void setTitle(String _title) {
        title = _title;
        toggleButton.setText((toggleButton.isSelected() ? Buttons.chevronDown : Buttons.chevronRight) + title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean toggle = !component.isVisible();
        toggleButton.setSelected(toggle);
        ComponentUtils.setVisible(component, toggle);
        setTitle(title);
    }

}
