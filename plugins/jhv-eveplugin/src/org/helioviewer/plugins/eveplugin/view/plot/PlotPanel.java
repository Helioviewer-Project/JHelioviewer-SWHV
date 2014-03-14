package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.helioviewer.plugins.eveplugin.lines.model.EVEDrawController;
import org.helioviewer.plugins.eveplugin.view.bandselector.BandSelectorPanel;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawGraphPane;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawValueRangePane;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;

public class PlotPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;
    
    private final EVEDrawController drawController;
    
    private final JPanel plotPane = new JPanel();
    private final LineDataSelectorPanel lineDataSelectorPane;
    private final ChartDrawGraphPane graphPane;
    private final ChartDrawValueRangePane valueRangePane;
    private ChartDrawIntervalPane intervalPane = null;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public PlotPanel(final String identifier, final String plotName) {
        lineDataSelectorPane = new LineDataSelectorPanel(identifier, plotName);
        
        drawController = new EVEDrawController(identifier);
        
        graphPane = new ChartDrawGraphPane(identifier);
        valueRangePane = new ChartDrawValueRangePane(drawController);
        
        initVisualComponents();
    }
    
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(200, 300));
        
        
        plotPane.setLayout(new BorderLayout());
        plotPane.add(graphPane, BorderLayout.CENTER);
        plotPane.add(valueRangePane, BorderLayout.LINE_END);
        
        add(plotPane, BorderLayout.CENTER);
        add(lineDataSelectorPane, BorderLayout.LINE_END);
    }
    
    public void setIntervalSlider(final ChartDrawIntervalPane intervalPane) {
        if (this.intervalPane != null) {
            plotPane.remove(this.intervalPane);    
        }
        
        this.intervalPane = intervalPane;
        
        if (intervalPane != null) {
            plotPane.add(intervalPane, BorderLayout.PAGE_END);
        }
    }
}
