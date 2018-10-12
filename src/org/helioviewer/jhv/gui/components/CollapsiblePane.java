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

/**
 * Panel managing a collapsible area.
 * <p>
 * This panel consists of a toggle button and one arbitrary component. Clicking
 * the toggle button will toggle the visibility of the component.
 */
@SuppressWarnings("serial")
public class CollapsiblePane extends JComponent implements ActionListener {

    final CollapsiblePaneButton toggleButton;
    private final JPanel component;
    private String title;

    public CollapsiblePane(String _title, Component managed, boolean startExpanded) {
        setLayout(new BorderLayout());

        toggleButton = new CollapsiblePaneButton();
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(UIGlobals.uiFontSmallBold);
        int height = toggleButton.getFontMetrics(UIGlobals.uiFontSmallBold).getHeight();
        toggleButton.setPreferredSize(new Dimension(0, height + 4));
        toggleButton.addActionListener(this);
        add(toggleButton, BorderLayout.PAGE_START);

        setTitle(_title);

        component = new JPanel(new BorderLayout());
        component.add(managed);
        ComponentUtils.setVisible(component, startExpanded);
        add(component, BorderLayout.CENTER);
    }

    void setTitle(String _title) {
        title = _title;
        if (toggleButton.isSelected())
            toggleButton.setText(Buttons.chevronDown + title);
        else
            toggleButton.setText(Buttons.chevronRight + title);
    }

    private void expand() {
        toggleButton.setSelected(true);
        ComponentUtils.setVisible(component, true);
        setTitle(title);
    }

    private void collapse() {
        toggleButton.setSelected(false);
        ComponentUtils.setVisible(component, false);
        setTitle(title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (component.isVisible()) {
            collapse();
        } else {
            expand();
        }
    }

}
