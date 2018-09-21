package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.UIGlobals;

// Class used for displaying information and statuses in a panel at the very bottom of JHV.
// The class manages two different areas in the panel, one at the lower left and one at the
// lower right corner. New plugins can be placed at one of the two areas.
// In addition, a status text can be displayed in the lower left corner.
@SuppressWarnings("serial")
public class StatusPanel extends JPanel {

    public static class StatusPlugin extends JLabel {

        protected StatusPlugin() {
            setFont(UIGlobals.UIFontMonoSmall);
        }

    }

    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    public enum Alignment {
        LEFT, RIGHT
    }

    public StatusPanel(int leftMargin, int rightMargin) {
        super(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        if (leftMargin != 0) {
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension(leftMargin, 0));
            leftPanel.add(spacer);
        }

        if (rightMargin != 0) {
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension(rightMargin, 0));
            rightPanel.add(spacer);
        }
    }

    public void addPlugin(StatusPlugin newPlugin, Alignment alignment) {
        if (alignment == Alignment.LEFT) {
            leftPanel.add(newPlugin);
            addLeftSpacer(10);
        } else {
            rightPanel.add(newPlugin, 0);
        }
    }

    public void addLeftSpacer(int width) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, 0));
        leftPanel.add(spacer);
    }

    public void removePlugin(StatusPlugin oldPlugin) {
        leftPanel.remove(oldPlugin);
        rightPanel.remove(oldPlugin);
    }

}
