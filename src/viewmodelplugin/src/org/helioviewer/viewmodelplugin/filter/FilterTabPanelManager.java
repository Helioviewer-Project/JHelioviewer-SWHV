package org.helioviewer.viewmodelplugin.filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This manager collects all filter control components and creates a panel where
 * all components will occur at the defined area.
 * <p>
 * There are three different areas: Top, Center, Bottom. Within the areas the
 * components will be occur below each other as they were added before.
 * 
 * @author Stephan Pagel
 */
public class FilterTabPanelManager {

    public enum Area {
        TOP, CENTER, BOTTOM
    }

    private LinkedList<Component> topList = new LinkedList<Component>();
    private LinkedList<Component> centerList = new LinkedList<Component>();
    private LinkedList<Component> bottomList = new LinkedList<Component>();

    /**
     * Adds a component to the manager. This function calls
     * {@link #add(Component, Area)} with CENTER as default value for the area.
     * 
     * @param comp
     *            Component to add.
     * @see #add(Component, Area)
     */
    public void add(Component comp) {
        add(comp, Area.CENTER);
    }

    /**
     * Adds a component to the manager at the given area. By using
     * {@link #createPanel()} a panel will be created where the component will
     * be occur at the specified area.
     * 
     * @param comp
     *            Component to add.
     * @param area
     *            Area where component should occur.
     */
    public void add(Component comp, Area area) {

        switch (area) {
        case TOP:
            topList.add(comp);
            break;
        case CENTER:
            centerList.add(comp);
            break;
        case BOTTOM:
            bottomList.add(comp);
            break;
        }
    }

    /**
     * Creates a panel which contains all added components add the specified
     * positions.
     * <p>
     * Components have to be added before creating the panel. Additional
     * components should not be added to the returned panel.
     * 
     * @return panel which contains the added components.
     */
    public JPanel createPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        JPanel center = new JPanel();
        JPanel bottom = new JPanel();

        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (Component comp : topList)
            top.add(comp);

        for (Component comp : centerList)
            center.add(comp);

        for (Component comp : bottomList)
            bottom.add(comp);

        return panel;
    }

    /**
     * Setup a new CompactPanel based on all Components added to the center
     * list, that implement the FilterAlignmentDetails interface
     * 
     * @see org.helioviewer.viewmodelplugin.filter# FilterAlignmentDetails
     * @return A JPanel containing all suitable components added to the center
     *         list
     */
    public JPanel createCompactPanel() {

        JPanel compactPanel = new JPanel() {

            private static final long serialVersionUID = 1L;

            /**
             * Override the setEnabled method in order to keep the containing
             * components' enabledState synced with the enabledState of this
             * component.
             */

            public void setEnabled(boolean enabled) {
                for (Component c : this.getComponents()) {
                    c.setEnabled(enabled);
                }

            }
        };

        compactPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 2, 5);

        for (Component comp : centerList) {
            if (comp instanceof FilterAlignmentDetails) {
                FilterAlignmentDetails details = (FilterAlignmentDetails) comp;

                /*
                 * This is the nasty part of it all The Layout had to be
                 * handcrafted and hardcoded :(
                 */
                if (details.getDetails() == FilterAlignmentDetails.POSITION_OPACITY) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 0;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                    c.gridy = 0;
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_SHARPEN) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 1;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_GAMMA) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 2;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_CONTRAST) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 3;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_COLORTABLES) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 4;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                } else if (details.getDetails() == FilterAlignmentDetails.POSITION_CHANNELMIXER) {
                    c.gridwidth = 3;
                    c.weightx = 1;
                    c.gridx = 0;
                    c.gridy = 5;
                    c.anchor = GridBagConstraints.WEST;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    compactPanel.add(comp, c);
                }
            }
        }
        return compactPanel;
    }

}
