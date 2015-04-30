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

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        isExpanded = !isExpanded;
        model.repackCollapsiblePanels();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

}
