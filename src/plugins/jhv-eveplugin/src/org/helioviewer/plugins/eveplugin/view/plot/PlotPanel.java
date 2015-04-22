package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawGraphPane;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;

public class PlotPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final JPanel plotPane = new JPanel();
    private final ChartDrawGraphPane graphPane;
    private final ChartDrawIntervalPane intervalPane = new ChartDrawIntervalPane();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public PlotPanel(final String plotName) {

        graphPane = new ChartDrawGraphPane();
        initVisualComponents();
    }

    private void initVisualComponents() {
        setLayout(new BorderLayout());
        // setMinimumSize(new Dimension(200, 300));

        plotPane.setLayout(new BorderLayout());
        plotPane.add(graphPane, BorderLayout.CENTER);
        plotPane.add(intervalPane, BorderLayout.PAGE_END);

        add(plotPane, BorderLayout.CENTER);

    }
}
