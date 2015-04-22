package org.helioviewer.plugins.eveplugin.view.plot;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDateSelectorTablePanel;

public class PlotsContainerPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    public static final String PLOT_IDENTIFIER_MASTER = "plot.identifier.master";
    public static final String PLOT_IDENTIFIER_SLAVE = "plot.identifier.slave";

    private final JSplitPane splitPane = new JSplitPane();

    private final PlotPanel plotOne = new PlotPanel("Plot 1: ");
    private final LineDateSelectorTablePanel lineDataSelectorTablePanel = new LineDateSelectorTablePanel();
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
            // plotOne.setIntervalSlider(intervalPane);
            add(plotOne, BorderLayout.CENTER);
        }
    }

    public boolean isPlot2Visible() {
        return isSecondPlotVisible;
    }
}
