package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.helioviewer.plugins.eveplugin.view.ControlsPanel;
import org.helioviewer.plugins.eveplugin.view.chart.ChartDrawIntervalPane;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorPanel;

public class PlotsContainerPanel extends JPanel implements LineDataSelectorModelListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    public static final String PLOT_IDENTIFIER_MASTER = "plot.identifier.master";
    public static final String PLOT_IDENTIFIER_SLAVE = "plot.identifier.slave";

    private final JSplitPane splitPane = new JSplitPane();
    private final ChartDrawIntervalPane intervalPane = new ChartDrawIntervalPane();

    private final PlotPanel plotOne = new PlotPanel("Plot 1: ");
    private final LineDataSelectorPanel lineDataSelectorPanelOne = new LineDataSelectorPanel("Plot 1:");
    // private final PlotPanel plotTwo = new PlotPanel(PLOT_IDENTIFIER_SLAVE,
    // "Plot 2: ");
    // private final LineDataSelectorPanel lineDataSelectorPanelTwo = new
    // LineDataSelectorPanel(PLOT_IDENTIFIER_SLAVE, "Plot 2:");

    private boolean isSecondPlotVisible = true;

    private static PlotsContainerPanel instance;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    private PlotsContainerPanel() {

        initVisualComponents();

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
        // splitPane.setBottomComponent(plotTwo);

        setPlot2Visible(false);
    }

    public void setPlot2Visible(final boolean visible) {
        if (isSecondPlotVisible == visible) {
            return;
        }

        // ControlsPanel.getSingletonInstance().removeLineDataSelector(lineDataSelectorPanelTwo);

        if (isSecondPlotVisible) {
            splitPane.remove(plotOne);
            remove(splitPane);
            // plotTwo.setIntervalSlider(null);
        } else {
            remove(plotOne);
            plotOne.setIntervalSlider(null);
        }

        isSecondPlotVisible = visible;

        if (isSecondPlotVisible) {
            // plotTwo.setIntervalSlider(intervalPane);
            // ControlsPanel.getSingletonInstance().addLineDataSelector(lineDataSelectorPanelTwo);
            splitPane.setTopComponent(plotOne);
            add(splitPane, BorderLayout.CENTER);
        } else {
            plotOne.setIntervalSlider(intervalPane);
            ControlsPanel.getSingletonInstance().addLineDataSelector(lineDataSelectorPanelOne);
            add(plotOne, BorderLayout.CENTER);
        }
    }

    public boolean isPlot2Visible() {
        return isSecondPlotVisible;
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
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }
}
