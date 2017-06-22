package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.data.event.SWEKTreeModelElement;
import org.helioviewer.jhv.gui.ComponentUtils;

@SuppressWarnings("serial")
class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (obj instanceof SWEKTreeModelElement) {
            return createLeaf((SWEKTreeModelElement) obj, tree.getBackground());
        } else {
            return super.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
        }
    }

    private static class TreeLabel extends JPanel {

        private final ImageIcon imageIcon;

        public TreeLabel(ImageIcon icon) {
            imageIcon = icon;
            int size = new JLabel("J").getPreferredSize().height;
            setPreferredSize(new Dimension(size, size));
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

    private static JPanel createLeaf(SWEKTreeModelElement element, Color back) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        ImageIcon icon = element.getIcon();
        if (icon != null) {
            panel.add(new TreeLabel(icon), BorderLayout.LINE_START);
        }

        JCheckBox checkBox = new JCheckBox(element.getName());
        checkBox.setSelected(element.isSelected());
        checkBox.setBackground(back);
        panel.add(checkBox, BorderLayout.CENTER);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
