package org.helioviewer.jhv.timelines.chart;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public final class PlotPanel extends JPanel {

    public PlotPanel() {
        setLayout(new BorderLayout());

        ChartDrawGraphPane graphPane = new ChartDrawGraphPane();
        JScrollPane scrollPane = new JScrollPane(graphPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        ChartDrawIntervalPane intervalPane = new ChartDrawIntervalPane();
        JPanel intervalWrapper = new JPanel(new BorderLayout());
        intervalWrapper.add(intervalPane);
        add(intervalWrapper, BorderLayout.PAGE_END);

        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
        scrollBar.addComponentListener(new ComponentAdapter() {
            private void alignIntervalPane() {
                int right = scrollBar.isVisible() ? scrollBar.getPreferredSize().width : 0;
                intervalWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, right));
            }

            @Override
            public void componentShown(ComponentEvent e) {
                alignIntervalPane();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                alignIntervalPane();
            }
        });
    }

}
