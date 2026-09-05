package org.helioviewer.jhv.event.info;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.helioviewer.jhv.gui.component.CollapsiblePane;

@SuppressWarnings("serial")
class DataCollapsiblePanel extends CollapsiblePane {

    private final Runnable repack;

    DataCollapsiblePanel(String title, JComponent managed, boolean startExpanded, Runnable _repack) {
        super(title, managed, startExpanded);
        repack = _repack;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        repack.run();
    }

}
