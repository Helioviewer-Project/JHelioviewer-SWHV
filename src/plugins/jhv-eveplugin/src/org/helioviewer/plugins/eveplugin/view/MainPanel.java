package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Date;

import javax.swing.JPanel;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.controller.ZoomController;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;
import org.helioviewer.plugins.eveplugin.view.plot.PlotsControlPanel;

//import org.helioviewer.plugins.eveplugin.model.PlotTimeSpace;

public class MainPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final PlotsContainerPanel plotsPane = new PlotsContainerPanel();

    private final PlotsControlPanel controlPane = new PlotsControlPanel(plotsPane);

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public MainPanel() {
        initVisualComponents();
        ZoomController.getSingletonInstance().setAvailableInterval(new Interval<Date>(new Date(), new Date()));
    }

    private void initVisualComponents() {
        setPreferredSize(new Dimension(300, 500));
        setLayout(new BorderLayout());

        add(plotsPane, BorderLayout.CENTER);
        add(controlPane, BorderLayout.PAGE_END);
    }

    public PlotsContainerPanel getPlotContainerPanel() {
        return plotsPane;
    }
}
