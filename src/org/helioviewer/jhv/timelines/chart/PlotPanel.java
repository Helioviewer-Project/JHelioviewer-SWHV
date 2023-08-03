package org.helioviewer.jhv.timelines.chart;

import java.awt.BorderLayout;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public final class PlotPanel extends JPanel {

    public PlotPanel() {
        setLayout(new BorderLayout());
        add(new ChartDrawGraphPane(), BorderLayout.CENTER);
        add(new ChartDrawIntervalPane(), BorderLayout.PAGE_END);
    }

}
