package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.plugins.swek.model.AbstractSWEKTreeModelElement;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelSupplier;

public class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    /** the serial verion UID */
    private static final long serialVersionUID = -436392148995692409L;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object whatToDisplay, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (whatToDisplay instanceof SWEKTreeModelEventType) {
            return createLeaf(((SWEKTreeModelEventType) whatToDisplay).getSwekEventType().getEventName(), whatToDisplay);
        } else if (whatToDisplay instanceof SWEKTreeModelSupplier) {
            return createLeaf(((SWEKTreeModelSupplier) whatToDisplay).getSwekSupplier().getSupplierDisplayName(), whatToDisplay);
        } else {
            return super.getTreeCellRendererComponent(tree, whatToDisplay, selected, expanded, leaf, row, hasFocus);
        }
    }

    class TreeLabel extends JPanel {
        ImageIcon imageIcon;

        public TreeLabel(ImageIcon icon) {
            super();
            this.imageIcon = icon;
        }

        @Override
        public void paintComponent(Graphics g) {
            //super.paintComponent(g);
            Image image = imageIcon.getImage();
            int minDim = getWidth() < getHeight() ? getWidth() : getHeight();
            int diffx = (getWidth() - minDim) / 2;
            int diffy = (getHeight() - minDim) / 2;
            g.drawImage(image, diffx, diffy, diffx + minDim, diffy + minDim, 0, 0, imageIcon.getIconWidth(), imageIcon.getIconHeight(), this);
        }
    }

    /**
     * Creates a leaf of the tree. This leaf will be a panel with the name of
     * the leaf and a checkbox indicating whether the leaf was selected or not.
     *
     * @param name
     *            The name of the leaf
     * @param whatToDisplay
     *            What to be displayed
     * @return The panel to be placed in the tree
     */
    private JPanel createLeaf(String name, Object whatToDisplay) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(((AbstractSWEKTreeModelElement) whatToDisplay).isCheckboxSelected());
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(checkBox, BorderLayout.LINE_START);
        ImageIcon icon = ((AbstractSWEKTreeModelElement) whatToDisplay).getIcon();
        if (icon != null) {
            JPanel tempPanel = new JPanel();
            tempPanel.setLayout(new BorderLayout());
            tempPanel.add(new TreeLabel(icon), BorderLayout.LINE_START);
            tempPanel.add(new JLabel(name), BorderLayout.CENTER);
            tempPanel.setOpaque(false);
            panel.add(tempPanel);
        } else {
            panel.add(new JLabel(name), BorderLayout.CENTER);
        }
        panel.setOpaque(false);
        return panel;
    }

}
