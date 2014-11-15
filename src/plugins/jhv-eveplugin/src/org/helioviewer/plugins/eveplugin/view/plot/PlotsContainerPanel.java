package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.plugins.eveplugin.lines.data.Band;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.view.ControlsPanel;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;

public class PlotsContainerPanel extends JPanel implements LineDataSelectorModelListener {// BandControllerListener,
                                                                                          // {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    public static final String PLOT_IDENTIFIER_MASTER = "plot.identifier.master";
    public static final String PLOT_IDENTIFIER_SLAVE = "plot.identifier.slave";

    private final JSplitPane splitPane = new JSplitPane();
    private final ChartDrawIntervalPane intervalPane = new ChartDrawIntervalPane();

    private final PlotPanel plotOne = new PlotPanel(PLOT_IDENTIFIER_MASTER, "Plot 1: ");
    private final LineDataSelectorPanel lineDataSelectorPanelOne = new LineDataSelectorPanel(PLOT_IDENTIFIER_MASTER, "Plot 1:");
    private final PlotPanel plotTwo = new PlotPanel(PLOT_IDENTIFIER_SLAVE, "Plot 2: ");
    private final LineDataSelectorPanel lineDataSelectorPanelTwo = new LineDataSelectorPanel(PLOT_IDENTIFIER_SLAVE, "Plot 2:");

    private boolean isSecondPlotVisible = true;

    private static PlotsContainerPanel instance;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private PlotsContainerPanel() {
        BandController.getSingletonInstance().registerBandManager(PLOT_IDENTIFIER_MASTER);
        BandController.getSingletonInstance().registerBandManager(PLOT_IDENTIFIER_SLAVE);

        initVisualComponents();

        // BandController.getSingletonInstance().addBandControllerListener(this);
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);
    }

    public static PlotsContainerPanel getSingletonInstance() {
        if (instance == null) {
            instance = new PlotsContainerPanel();
        }
        return instance;
    }

    private void initVisualComponents() {
        setLayout(new BorderLayout());

        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setTopComponent(plotOne);
        splitPane.setBottomComponent(plotTwo);

        setPlot2Visible(false);
    }

    public void setPlot2Visible(final boolean visible) {
        if (isSecondPlotVisible == visible) {
            return;
        }

        ControlsPanel.getSingletonInstance().removeLineDataSelector(lineDataSelectorPanelTwo);

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
            ControlsPanel.getSingletonInstance().addLineDataSelector(lineDataSelectorPanelTwo);
            splitPane.setTopComponent(plotOne);
            add(splitPane, BorderLayout.CENTER);
        } else {
            plotOne.setIntervalSlider(intervalPane);
            ControlsPanel.getSingletonInstance().addLineDataSelector(lineDataSelectorPanelOne);
            add(plotOne, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        ImageViewerGui.getSingletonInstance().getContentPane().revalidate();
        ImageViewerGui.getSingletonInstance().getContentPane().repaint();
    }

    public boolean isPlot2Visible() {
        return isSecondPlotVisible;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Band Controller Listener
    // //////////////////////////////////////////////////////////////////////////////

    public void bandAdded(final Band band, final String identifier) {
    }

    public void bandRemoved(final Band band, final String identifier) {
        if (identifier.equals(PLOT_IDENTIFIER_SLAVE)) {
            final int numberOfBands = BandController.getSingletonInstance().getNumberOfAvailableBands(identifier);
            setPlot2Visible(numberOfBands > 0);
        }
    }

    public void bandUpdated(final Band band, final String identifer) {
    }

    public void bandGroupChanged(final String identifer) {
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        if (element.getPlotIdentifier().equals(PLOT_IDENTIFIER_SLAVE)) {
            List<LineDataSelectorElement> allElements = LineDataSelectorModel.getSingletonInstance().getAllLineDataSelectorElements(
                    element.getPlotIdentifier());
            if (allElements != null) {
                int numberOfLines = LineDataSelectorModel.getSingletonInstance()
                        .getAllLineDataSelectorElements(element.getPlotIdentifier()).size();
                setPlot2Visible(numberOfLines > 0);
            } else {
                setPlot2Visible(false);
            }
        }

    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }
}
