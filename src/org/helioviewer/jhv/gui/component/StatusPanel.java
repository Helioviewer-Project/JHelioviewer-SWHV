package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.UIGlobals;

// Class used for displaying information and statuses in a panel at the very bottom of JHV.
// The class manages two different areas in the panel, one at the lower left and one at the
// lower right corner. New plugins can be placed at one of the two areas.
// In addition, a status text can be displayed in the lower left corner.
@SuppressWarnings("serial")
public final class StatusPanel extends JPanel {

    @SuppressWarnings("this-escape")
    public static class StatusPlugin extends JLabel {
        private int[] labelRanges;

        protected StatusPlugin() {
            setFont(UIGlobals.uiFontMonoSmall);
        }

        protected void setLabelRanges(int[] ranges) {
            labelRanges = ranges;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (labelRanges == null) {
                super.paintComponent(graphics);
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setFont(getFont());
                FontMetrics metrics = g.getFontMetrics();
                Insets insets = getInsets();
                int x = insets.left;
                int y = insets.top + (getHeight() - insets.top - insets.bottom - metrics.getHeight()) / 2 + metrics.getAscent();
                String text = getText();
                int start = 0;
                for (int index = 0; index <= labelRanges.length; index++) {
                    int end = index == labelRanges.length ? text.length() : labelRanges[index];
                    g.setColor(index % 2 == 0 ? getForeground() : UIGlobals.secondaryTextColor);
                    String part = text.substring(start, end);
                    g.drawString(part, x, y);
                    x += metrics.stringWidth(part);
                    start = end;
                }
            } finally {
                g.dispose();
            }
        }
    }

    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    public enum Alignment {
        LEFT, RIGHT
    }

    public StatusPanel(int leftMargin, int rightMargin) {
        super(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")));
        setVisible(Boolean.parseBoolean(Settings.getProperty("display.statusbar.visible")));

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

    public void setStatusBarVisible(boolean visible) {
        Settings.setProperty("display.statusbar.visible", Boolean.toString(visible));
        setVisible(visible);
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
