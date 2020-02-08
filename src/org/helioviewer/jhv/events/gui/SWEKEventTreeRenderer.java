package org.helioviewer.jhv.events.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.events.gui.filter.FilterDialog;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.JHVButton;

@SuppressWarnings("serial")
class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    private final JTree tree;

    SWEKEventTreeRenderer(JTree _tree) {
        tree = _tree;
    }

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

        TreeLabel(ImageIcon icon) {
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

    private JPanel createLeaf(SWEKTreeModelElement element, Color back) {
        JPanel leaf = new JPanel(new BorderLayout());
        leaf.setOpaque(false);

        if (element instanceof SWEKGroup) {
            leaf.add(new TreeLabel(((SWEKGroup) element).getIcon()), BorderLayout.LINE_START);
        }

        JCheckBox checkBox = new JCheckBox(element.getName());
        checkBox.setSelected(element.isSelected());
        checkBox.setBackground(back);
        checkBox.addActionListener(e -> {
            element.activate(checkBox.isSelected());
            tree.repaint();
        });
        leaf.add(checkBox, BorderLayout.CENTER);

        if (element instanceof SWEKSupplier) {
            FilterDialog filterDialog = ((SWEKSupplier) element).getFilterDialog();
            if (filterDialog != null) {
                JHVButton filterButton = new JHVButton("Filter");
                filterButton.addActionListener(e -> filterDialog.setVisible(true));
                filterButton.addMouseListener(new FilterMouseAdapter(filterDialog));
                leaf.setPreferredSize(new Dimension(250, filterButton.getPreferredSize().height)); //!
                leaf.add(filterButton, BorderLayout.LINE_END);
            }
        }

        ComponentUtils.smallVariant(leaf);
        return leaf;
    }

    private static class FilterMouseAdapter extends MouseAdapter {

        private final FilterDialog filterDialog;

        FilterMouseAdapter(FilterDialog _filterDialog) {
            filterDialog = _filterDialog;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Point pressedLocation = e.getLocationOnScreen();
            Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
            filterDialog.setLocation(windowLocation);
        }
    }

}
