package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class used for displaying information and statuses in a panel at the very
 * bottom of JHV.
 *
 * <p>
 * The class manages two different areas in the panel, one at the lower left and
 * one at the lower right corner. New plugins can be placed at one of the two
 * areas.
 *
 * <p>
 * In addition, a status text can be displayed in the lower left corner.
 *
 * @author Markus Langenberg
 */
public class StatusPanel extends JPanel {

    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private static JLabel statusInfoLabel = new JLabel("");

    public enum Alignment {
        LEFT, RIGHT
    };

    /**
     * Default constructor
     *
     * @param leftMargin
     *            left margin. If greater zero, the status text will be
     *            displayed here.
     * @param rightMargin
     *            right margin
     */
    public StatusPanel(int leftMargin, int rightMargin) {
        super(new BorderLayout());

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        if (leftMargin != 0) {
            statusInfoLabel.setPreferredSize(new Dimension(leftMargin, 1));
            leftPanel.add(statusInfoLabel);
        }

        if (rightMargin != 0) {
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension(rightMargin, 1));
            rightPanel.add(spacer);
        }
    }

    /**
     * Adds a new plugin to the status panel.
     *
     * @param newPlugin
     *            Plugin to add
     * @param alignment
     *            Alignment of the new plugin, can be either LEFT or RIGHT
     */
    public void addPlugin(JLabel newPlugin, Alignment alignment) {
        if (alignment == Alignment.LEFT) {
            leftPanel.add(newPlugin);
        } else {
            rightPanel.add(newPlugin, 0);
        }
    }

    /**
     * Removes a plugin from the status panel.
     *
     * @param oldPlugin
     *            Plugin to remove
     */
    public void removePlugin(JLabel oldPlugin) {
        leftPanel.remove(oldPlugin);
        rightPanel.remove(oldPlugin);
    }

}
