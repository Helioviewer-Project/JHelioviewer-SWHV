package org.helioviewer.jhv.plugins.eveplugin.view.chart;

import java.awt.BorderLayout;

import javax.swing.JPanel;

//Class will not be serialized so we suppress the warnings
@SuppressWarnings("serial")
public class PlotPanel extends JPanel {

    public PlotPanel() {
        JPanel plotPane = new JPanel();
        setLayout(new BorderLayout());
        plotPane.setLayout(new BorderLayout());
        plotPane.add(new ChartDrawGraphPane(), BorderLayout.CENTER);
        plotPane.add(new ChartDrawIntervalPane(), BorderLayout.PAGE_END);
        add(plotPane, BorderLayout.CENTER);
    }

}
