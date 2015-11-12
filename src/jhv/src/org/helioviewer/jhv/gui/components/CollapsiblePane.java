package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;

/**
 * Panel managing a collapsible area.
 *
 * <p>
 * This panel consists of a toggle button and one arbitrary component. Clicking
 * the toggle button will toggle the visibility of the component.
 *
 * @author Markus Langenberg
 */
@SuppressWarnings("serial")
public class CollapsiblePane extends JComponent implements ActionListener {

    private static final ImageIcon expandedIcon = IconBank.getIcon(JHVIcon.DOWN2);
    private static final ImageIcon collapsedIcon = IconBank.getIcon(JHVIcon.RIGHT2);

    protected CollapsiblePaneButton toggleButton;
    private final JPanel component;
    protected JPanel topButtonsPanel;

    /**
     * Default constructor.
     *
     * @param title
     *            Text on the toggle button
     * @param component
     *            Component to manage
     * @param startExpanded
     *            if true, the component will be visible right from the start
     */
    public CollapsiblePane(String title, Component component, boolean startExpanded) {
        setLayout(new BorderLayout());

        toggleButton = new CollapsiblePaneButton(title);
        toggleButton.setBorderPainted(false);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        toggleButton.setSelected(startExpanded);
        toggleButton.setFont(UIGlobals.UIFontSmallBold);
        if (startExpanded) {
            toggleButton.setIcon(IconBank.getIcon(JHVIcon.DOWN2));
        } else {
            toggleButton.setIcon(IconBank.getIcon(JHVIcon.RIGHT2));
        }
        toggleButton.setPreferredSize(new Dimension(0, UIGlobals.UIFontSmallBold.getSize() + 4));
        toggleButton.addActionListener(this);

        this.component = new JPanel(new BorderLayout());
        this.component.add(component);
        this.component.setVisible(startExpanded);
        add(this.component, BorderLayout.CENTER);
        setButtons();
    }

    public void setButtons() {
        topButtonsPanel = new JPanel();
        topButtonsPanel.setLayout(new BorderLayout());
        topButtonsPanel.add(toggleButton, BorderLayout.NORTH);
        add(topButtonsPanel, BorderLayout.PAGE_START);
    }

    /**
     * Sets the text on the toggle button
     *
     * @param title
     *            Text on the toggle button
     * */
    public void setTitle(final String title) {
        toggleButton.setText(title);
    }

    /**
     * Expands the pane.
     */
    public void expand() {
        toggleButton.setSelected(true);
        component.setVisible(true);
        toggleButton.setIcon(expandedIcon);
    }

    /**
     * Collapses the pane.
     */
    public void collapse() {
        toggleButton.setSelected(false);
        component.setVisible(false);
        toggleButton.setIcon(collapsedIcon);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (component.isVisible()) {
            collapse();
        } else {
            expand();
        }
    }

    public boolean isCollapsed() {
        return !component.isVisible();
    }

}
