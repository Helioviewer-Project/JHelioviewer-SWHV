package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UIGlobals;

// This panel consists of a toggle button and one arbitrary component. Clicking
// the toggle button will toggle the visibility of the component.
@SuppressWarnings({"serial", "this-escape"})
public class CollapsiblePane extends JComponent implements ActionListener {

    final CollapsiblePaneButton toggleButton;
    private final JComponent managed;
    private final JPanel header;
    private String title;

    public CollapsiblePane(String _title, JComponent _managed, boolean startExpanded) {
        setLayout(new BorderLayout());

        managed = _managed;
        ComponentUtils.setVisible(managed, startExpanded);

        toggleButton = new CollapsiblePaneButton();
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(UIGlobals.uiFontSmallBold);
        toggleButton.addActionListener(this);
        setTitle(_title);

        header = new JPanel(new BorderLayout());
        header.add(toggleButton, BorderLayout.CENTER);

        add(header, BorderLayout.PAGE_START);
        add(managed, BorderLayout.CENTER);
    }

    void addHeaderComponent(JComponent component) {
        header.add(component, BorderLayout.LINE_END);
    }

    void setTitle(String _title) {
        title = _title;
        toggleButton.setText((toggleButton.isSelected() ? Buttons.chevronDown : Buttons.chevronRight) + title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean toggle = !managed.isVisible();
        ComponentUtils.setVisible(managed, toggle);
        toggleButton.setSelected(toggle);
        setTitle(title);
    }

}
