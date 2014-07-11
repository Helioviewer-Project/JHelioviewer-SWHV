package org.helioviewer.jhv.plugins.swek.view;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;

public class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    /** the serial verion UID */
    private static final long serialVersionUID = -436392148995692409L;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object whatToDisplay, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {
        if (whatToDisplay instanceof SWEKEventType) {
            return new JLabel("eventtype : " + ((SWEKEventType) whatToDisplay).getEventName());
        } else if (whatToDisplay instanceof SWEKSupplier) {
            return new JLabel("supplier : " + ((SWEKSupplier) whatToDisplay).getSupplierName());
        } else {
            return super.getTreeCellRendererComponent(tree, whatToDisplay, selected, expanded, leaf, row, hasFocus);
        }
    }

}
