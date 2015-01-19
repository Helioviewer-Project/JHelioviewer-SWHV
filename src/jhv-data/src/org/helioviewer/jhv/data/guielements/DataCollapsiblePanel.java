package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.event.ActionEvent;

import org.helioviewer.jhv.data.guielements.model.DataCollapsiblePanelModel;
import org.helioviewer.jhv.gui.components.CollapsiblePane;

public class DataCollapsiblePanel extends CollapsiblePane {
    private boolean isExpanded;

    private final DataCollapsiblePanelModel model;

    public DataCollapsiblePanel(String title, Component component, boolean startExpanded, DataCollapsiblePanelModel model) {
        super(title, component, startExpanded);
        this.model = model;
        isExpanded = startExpanded;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 4318197859567084201L;

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        isExpanded = !isExpanded;
        model.repackCollasiblePanels();
    }

    public boolean isExpanded() {
        return isExpanded;
    }
}
