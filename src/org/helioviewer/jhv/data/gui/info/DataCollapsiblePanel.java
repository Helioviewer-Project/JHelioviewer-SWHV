package org.helioviewer.jhv.data.gui.info;

import java.awt.Component;
import java.awt.event.ActionEvent;

import org.helioviewer.jhv.gui.components.CollapsiblePane;

@SuppressWarnings("serial")
class DataCollapsiblePanel extends CollapsiblePane {

    private boolean isExpanded;

    private final DataCollapsiblePanelModel model;

    public DataCollapsiblePanel(String title, Component component, boolean startExpanded, DataCollapsiblePanelModel _model) {
        super(title, component, startExpanded);
        isExpanded = startExpanded;
        model = _model;
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
