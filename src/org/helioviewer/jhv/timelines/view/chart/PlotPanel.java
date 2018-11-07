package org.helioviewer.jhv.timelines.view.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class PlotPanel extends JPanel {

    public PlotPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(-1, 50));

        add(new ChartDrawGraphPane(), BorderLayout.CENTER);
        add(new ChartDrawIntervalPane(), BorderLayout.PAGE_END);
    }

}
