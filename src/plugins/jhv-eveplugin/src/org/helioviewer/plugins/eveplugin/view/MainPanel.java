package org.helioviewer.plugins.eveplugin.view;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.helioviewer.plugins.eveplugin.view.plot.PlotsContainerPanel;

//import org.helioviewer.plugins.eveplugin.model.PlotTimeSpace;

public class MainPanel extends JPanel {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private static final long serialVersionUID = 1L;

    private final PlotsContainerPanel plotsPane = PlotsContainerPanel.getSingletonInstance();

    // private final PlotsControlPanel controlPane = new PlotsControlPanel();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public MainPanel() {
        initVisualComponents();

    }

    private void initVisualComponents() {
        // setPreferredSize(new Dimension(300, 250));
        setLayout(new BorderLayout());

        add(plotsPane, BorderLayout.CENTER);
        // add(controlPane, BorderLayout.PAGE_END);
    }

    public PlotsContainerPanel getPlotContainerPanel() {
        return plotsPane;
    }
}
