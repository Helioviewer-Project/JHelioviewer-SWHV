package org.helioviewer.jhv.plugins.swek.view;

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

import org.helioviewer.jhv.data.event.SWEKGroup;
import org.helioviewer.jhv.data.event.SWEKSupplier;
import org.helioviewer.jhv.data.event.SWEKTreeModelElement;
import org.helioviewer.jhv.data.gui.filter.FilterDialog;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
class SWEKEventTreeRenderer extends DefaultTreeCellRenderer {

    private final EventTypePanelModel model;
    private final JTree tree;

    public SWEKEventTreeRenderer(JTree _tree, EventTypePanelModel _model) {
        tree = _tree;
        model = _model;
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        ImageIcon icon = element.getIcon();
        if (icon != null) {
            panel.add(new TreeLabel(icon), BorderLayout.LINE_START);
        }

        JCheckBox checkBox = new JCheckBox(element.getName());
        checkBox.setSelected(element.isSelected());
        checkBox.setBackground(back);
        checkBox.addActionListener(e -> {
            boolean selected = checkBox.isSelected();
            if (element instanceof SWEKGroup)
                model.selectGroup((SWEKGroup) element, selected);
            else
                model.selectSupplier((SWEKSupplier) element, selected);
            tree.repaint();
        });
        panel.add(checkBox, BorderLayout.CENTER);

        FilterDialog filterDialog = element.getFilterDialog();
        if (filterDialog != null) {
            JideButton filterButton = new JideButton("Filter");
            filterButton.addActionListener(e -> filterDialog.setVisible(true));
            filterButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Point pressedLocation = e.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                }
            });
            panel.add(filterButton, BorderLayout.LINE_END);
        }

        ComponentUtils.smallVariant(panel);
        return panel;
    }

}
