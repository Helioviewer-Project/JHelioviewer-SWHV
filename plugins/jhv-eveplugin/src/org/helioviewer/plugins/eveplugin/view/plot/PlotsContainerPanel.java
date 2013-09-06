package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.plugins.eveplugin.controller.Band;
import org.helioviewer.plugins.eveplugin.controller.BandController;
import org.helioviewer.plugins.eveplugin.controller.BandControllerListener;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;

public class PlotsContainerPanel extends JPanel implements BandControllerListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////
    
    private static final long serialVersionUID = 1L;
    
    public static final String PLOT_IDENTIFIER_MASTER = "plot.identifier.master";
    public static final String PLOT_IDENTIFIER_SLAVE = "plot.identifier.slave";
    
    private final JSplitPane splitPane = new JSplitPane();
    private final ChartDrawIntervalPane intervalPane = new ChartDrawIntervalPane();
    
    private final PlotPanel plotOne = new PlotPanel(PLOT_IDENTIFIER_MASTER, "Plot 1: ");
    private final PlotPanel plotTwo = new PlotPanel(PLOT_IDENTIFIER_SLAVE, "Plot 2: ");
    
    private boolean isSecondPlotVisible = true;
    
    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////
    
    public PlotsContainerPanel() {
        BandController.getSingletonInstance().registerBandManager(PLOT_IDENTIFIER_MASTER);
        BandController.getSingletonInstance().registerBandManager(PLOT_IDENTIFIER_SLAVE);
        
        initVisualComponents();
        
        BandController.getSingletonInstance().addBandControllerListener(this);
    }
    
    private void initVisualComponents() {
        setLayout(new BorderLayout());
        
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(plotOne);
        splitPane.setBottomComponent(plotTwo);
        
        setPlot2Visible(false);
    }
    
    public void setPlot2Visible(final boolean visible) {
        if (isSecondPlotVisible == visible) {
            return;
        }
        
        if (isSecondPlotVisible) {
            splitPane.remove(plotOne);
            remove(splitPane);
            
            plotTwo.setIntervalSlider(null);
        } else {
            remove(plotOne);
            plotOne.setIntervalSlider(null);
        }
        
        isSecondPlotVisible = visible;
        
        if (isSecondPlotVisible) {
            plotTwo.setIntervalSlider(intervalPane);
            
            splitPane.setTopComponent(plotOne);
            add(splitPane, BorderLayout.CENTER);
        } else {
            plotOne.setIntervalSlider(intervalPane);
            
            add(plotOne, BorderLayout.CENTER);
        }
        
        ImageViewerGui.getSingletonInstance().getContentPane().revalidate();
    }
    
    public boolean isPlot2Visible() {
        return isSecondPlotVisible;
    }
    
    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {}

    public void bandRemoved(final Band band, final String identifier) {
        if (identifier.equals(PLOT_IDENTIFIER_SLAVE)) {
            final int numberOfBands = BandController.getSingletonInstance().getNumberOfAvailableBands(identifier);
            setPlot2Visible(numberOfBands > 0);
        }
    }

    public void bandUpdated(final Band band, final String identifer) {}

    public void bandGroupChanged(final String identifer) {}
}
