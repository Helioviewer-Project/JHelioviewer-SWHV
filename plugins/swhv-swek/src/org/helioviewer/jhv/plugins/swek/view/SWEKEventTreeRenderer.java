package org.helioviewer.jhv.plugins.swek.view;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelSupplier;

public class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    /** the serial verion UID */
    private static final long serialVersionUID = -436392148995692409L;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object whatToDisplay, boolean selected, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {
        if (whatToDisplay instanceof SWEKTreeModelEventType) {
            return createLeaf(((SWEKTreeModelEventType) whatToDisplay).getSwekEventType().getEventName());
        } else if (whatToDisplay instanceof SWEKTreeModelSupplier) {
            return createLeaf(((SWEKTreeModelSupplier) whatToDisplay).getSwekSupplier().getSupplierName());
        } else {
            return super.getTreeCellRendererComponent(tree, whatToDisplay, selected, expanded, leaf, row, hasFocus);
        }
    }

    private JPanel createLeaf(String name) {
        JPanel panel = new JPanel();
        panel.add(new JCheckBox());
        panel.add(new JLabel(name));
        // panel.setBackground(Color.WHITE);
        panel.setOpaque(false);
        return panel;
    }
}
