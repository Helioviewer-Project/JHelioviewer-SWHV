package org.helioviewer.jhv.gui.components;

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
            setFont(UIGlobals.uiFontMonoSmall);
        }
    }

    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    public enum Alignment {
        LEFT, RIGHT
    }

    public StatusPanel(int leftMargin, int rightMargin) {
        super(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().brighter()));

        add(leftPanel, BorderLayout.LINE_START);
        add(rightPanel, BorderLayout.LINE_END);

        if (leftMargin != 0) {
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension(leftMargin, -1));
            leftPanel.add(spacer);
        }

        if (rightMargin != 0) {
            JPanel spacer = new JPanel();
            spacer.setPreferredSize(new Dimension(rightMargin, -1));
            rightPanel.add(spacer);
        }
    }

    public void addPlugin(StatusPlugin newPlugin, Alignment alignment) {
        if (alignment == Alignment.LEFT) {
            leftPanel.add(newPlugin);
            addLeftSpacer(10);
        } else {
            rightPanel.add(newPlugin, 0);
            addRightSpacer(10);
        }
    }

    private void addLeftSpacer(int width) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, -1));
        leftPanel.add(spacer);
    }

    private void addRightSpacer(int width) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(width, -1));
        rightPanel.add(spacer, 0);
    }

}
