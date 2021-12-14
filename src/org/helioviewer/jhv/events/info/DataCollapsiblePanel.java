package org.helioviewer.jhv.events.info;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.components.CollapsiblePane;

@SuppressWarnings("serial")
class DataCollapsiblePanel extends CollapsiblePane {

    private boolean isExpanded;

    private final DataCollapsiblePanelModel model;

    DataCollapsiblePanel(String title, JComponent managed, boolean startExpanded, DataCollapsiblePanelModel _model) {
        super(title, managed, startExpanded);
        isExpanded = startExpanded;
        model = _model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        isExpanded = !isExpanded;
        model.repackCollapsiblePanels();
    }

    boolean isExpanded() {
        return isExpanded;
    }

}
