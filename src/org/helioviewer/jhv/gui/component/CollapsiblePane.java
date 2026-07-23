package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.UIGlobals;

// This panel consists of a toggle button and one arbitrary component. Clicking
// the toggle button will toggle the visibility of the component.
@SuppressWarnings({"serial", "this-escape"})
public class CollapsiblePane extends JComponent implements ActionListener {

    final CollapsiblePaneButton toggleButton;
    private final JComponent managed;
    private String title;

    public CollapsiblePane(String _title, JComponent _managed, boolean startExpanded) {
        this(_title, _managed, startExpanded, false);
    }

    // child=true renders a subordinate (nested) section: regular weight instead of bold,
    // so it reads as a child of the bold parent header it sits indented beneath.
    public CollapsiblePane(String _title, JComponent _managed, boolean startExpanded, boolean child) {
        setLayout(new BorderLayout());

        managed = _managed;
        ComponentUtils.setVisible(managed, startExpanded);

        toggleButton = new CollapsiblePaneButton();
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(child ? UIGlobals.uiFontSmall : UIGlobals.uiFontSmallBold);
        toggleButton.addActionListener(this);
        setTitle(_title);

        add(toggleButton, BorderLayout.PAGE_START);
        add(managed, BorderLayout.CENTER);
    }

    public void setTitle(String _title) {
        title = _title;
        toggleButton.setText((toggleButton.isSelected() ? Buttons.chevronDown : Buttons.chevronRight) + title);
    }

    public void setExpanded(boolean expanded) {
        ComponentUtils.setVisible(managed, expanded);
        toggleButton.setSelected(expanded);
        setTitle(title);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setExpanded(!managed.isVisible());
    }

}
