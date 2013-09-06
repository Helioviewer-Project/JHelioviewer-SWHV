package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.helioviewer.plugins.eveplugin.controller.DrawController;
import org.helioviewer.plugins.eveplugin.view.bandselector.BandSelectorPanel;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawGraphPane;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawValueRangePane;

public class PlotPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;
    
    private final DrawController drawController;
    
    private final JPanel plotPane = new JPanel();
    private final BandSelectorPanel bandSelectorPane;
    private final ChartDrawGraphPane graphPane;
    private final ChartDrawValueRangePane valueRangePane;
    private ChartDrawIntervalPane intervalPane = null;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public PlotPanel(final String identifier, final String plotName) {
        bandSelectorPane = new BandSelectorPanel(identifier, plotName);
        
        drawController = new DrawController(identifier);
        
        graphPane = new ChartDrawGraphPane(drawController);
        valueRangePane = new ChartDrawValueRangePane(drawController);
        
        initVisualComponents();
    }
    
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(200, 80));
        
        
        plotPane.setLayout(new BorderLayout());
        plotPane.add(graphPane, BorderLayout.CENTER);
        plotPane.add(valueRangePane, BorderLayout.LINE_END);
        
        add(plotPane, BorderLayout.CENTER);
        add(bandSelectorPane, BorderLayout.LINE_END);
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
