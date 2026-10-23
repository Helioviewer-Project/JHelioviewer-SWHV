package org.helioviewer.jhv.gui.component;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.gui.MainFrame;

// The canvas and optional collapsible Timelines plot share a vertical split pane.
@SuppressWarnings("serial")
public final class MainContentPanel extends JPanel {

    private static final int DIVIDER_SIZE = 5;
    private static final double NORMAL_RESIZE_WEIGHT = 0.75;

    private final JSplitPane splitPane;
    private final JPanel timelineContainer;
    private final CollapsiblePane collapsiblePane;
    private final CollapsiblePaneButton maximizeButton;

    private boolean timelineMaximized;
    private int normalDividerLocation;

    public MainContentPanel(Component mainComponent) {
        timelineContainer = new JPanel(new BorderLayout());
        collapsiblePane = new CollapsiblePane("Timelines", timelineContainer, !"false".equals(Settings.getProperty("display.plugins")));
        collapsiblePane.toggleButton.addActionListener(e -> updateLayout());

        maximizeButton = new CollapsiblePaneButton();
        maximizeButton.setFont(Buttons.getMaterialFont(12));
        maximizeButton.setHorizontalAlignment(SwingConstants.CENTER);
        maximizeButton.setPreferredSize(new Dimension(28, maximizeButton.getPreferredSize().height));
        maximizeButton.addActionListener(e -> toggleTimelineMaximized());
        collapsiblePane.addHeaderComponent(maximizeButton);
        updateMaximizeButton();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerSize(0);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(NORMAL_RESIZE_WEIGHT);

        splitPane.setTopComponent(mainComponent);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension());
        add(splitPane, BorderLayout.CENTER);
    }

    public void setTimelinesPanel(@Nullable JComponent panel) {
        timelineContainer.removeAll();
        if (panel != null)
            timelineContainer.add(panel, BorderLayout.CENTER);
        updateLayout();
    }

    private void updateLayout() {
        boolean hasContent = timelineContainer.getComponentCount() != 0;
        if ((!hasContent || !collapsiblePane.toggleButton.isSelected()) && timelineMaximized)
            restoreTimelineSize();

        splitPane.remove(collapsiblePane);
        remove(collapsiblePane);
        splitPane.setDividerSize(0);

        if (!hasContent) {
            revalidate();
            repaint();
            return;
        }

        boolean isSelected = collapsiblePane.toggleButton.isSelected();
        if (isSelected) {
            splitPane.setBottomComponent(collapsiblePane);
            if (timelineMaximized) {
                splitPane.setDividerLocation(0);
            } else {
                splitPane.setDividerSize(DIVIDER_SIZE);
            }
        } else {
            add(collapsiblePane, BorderLayout.PAGE_END);
        }
        maximizeButton.setVisible(isSelected);
        Settings.setProperty("display.plugins", Boolean.toString(isSelected));

        revalidate();
        repaint();
    }

    private void toggleTimelineMaximized() {
        if (timelineMaximized) {
            restoreTimelineSize();
        } else {
            normalDividerLocation = splitPane.getDividerLocation();
            MainFrame.setRenderSurfaceVisible(false);
            splitPane.setResizeWeight(0);
            splitPane.setDividerSize(0);
            splitPane.setDividerLocation(0);
            timelineMaximized = true;
            updateMaximizeButton();
            revalidate();
            repaint();
        }
    }

    private void restoreTimelineSize() {
        splitPane.setResizeWeight(NORMAL_RESIZE_WEIGHT);
        splitPane.setDividerSize(DIVIDER_SIZE);
        splitPane.setDividerLocation(normalDividerLocation);
        timelineMaximized = false;
        updateMaximizeButton();
        revalidate();
        repaint();
        EventQueue.invokeLater(() -> {
            if (!timelineMaximized)
                MainFrame.setRenderSurfaceVisible(true);
        });
    }

    private void updateMaximizeButton() {
        maximizeButton.setSelected(true);
        maximizeButton.setText((timelineMaximized ? MaterialDesign.CHEVRON_DOWN : MaterialDesign.CHEVRON_UP).toString());
        maximizeButton.setToolTipText(timelineMaximized ? "Restore panel size" : "Maximize panel");
    }

}
