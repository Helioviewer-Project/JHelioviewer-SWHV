package org.helioviewer.jhv.plugins.swek;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.IdentityHashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.events.SWEKGroup;
import org.helioviewer.jhv.events.SWEKSupplier;
import org.helioviewer.jhv.events.filter.FilterDialog;
import org.helioviewer.jhv.gui.components.base.BusyIndicator;

import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
final class SWEKTreePane extends JPanel {

    private static final int RIGHT_ALIGNMENT = 300;

    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final IdentityHashMap<SWEKGroup, Component> groupComponents = new IdentityHashMap<>();
    private final IdentityHashMap<SWEKGroup, TreePath> groupPaths = new IdentityHashMap<>();
    private final IdentityHashMap<SWEKSupplier, Component> supplierComponents = new IdentityHashMap<>();
    private final Timer loadingTimer;

    SWEKTreePane(DefaultTreeModel _treeModel) {
        super(new BorderLayout());
        treeModel = _treeModel;

        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new Renderer());
        tree.setCellEditor(new Editor());
        // tree.setRowHeight(0); // force calculation of nodes heights
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);

        loadingTimer = new Timer(500, e -> repaintBusyGroups());

        setBorder(BorderFactory.createEmptyBorder());
        add(tree, BorderLayout.CENTER);
    }

    private void repaintBusyGroups() {
        boolean anyBusy = false;
        Enumeration<?> children = ((DefaultMutableTreeNode) treeModel.getRoot()).children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (child instanceof SWEKGroup group && group.isDownloading()) {
                anyBusy = true;
                repaintGroup(group);
            }
        }

        if (!anyBusy)
            loadingTimer.stop();
    }

    private void repaintGroup(SWEKGroup group) {
        Rectangle bounds = tree.getPathBounds(groupPaths.computeIfAbsent(group, g -> new TreePath(g.getPath())));
        if (bounds != null)
            tree.repaint(bounds);
    }

    private Component componentFor(Object value) {
        Component component = null;
        if (value instanceof SWEKGroup group) {
            if (group.isDownloading() && !loadingTimer.isRunning())
                loadingTimer.start();
            component = groupComponents.computeIfAbsent(group, SWEKTreePane::createGroupComponent);
            if (component instanceof JPanel panel && panel.getComponentCount() > 1)
                panel.getComponent(1).setVisible(group.isDownloading());
        } else if (value instanceof SWEKSupplier supplier) {
            component = supplierComponents.computeIfAbsent(supplier, SWEKTreePane::createSupplierComponent);
            if (component instanceof JPanel panel && panel.getComponent(0) instanceof JCheckBox checkBox)
                checkBox.setSelected(supplier.isActive());
        }

        if (component != null)
            setEnabledRecursively(component, tree.isEnabled());
        return component;
    }

    private static Component createGroupComponent(SWEKGroup group) {
        JLabel label = new JLabel(group.getName());
        int size = label.getPreferredSize().height;
        ImageIcon icon = SWEKIconBank.getIcon(group.getIconKey());
        label.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));

        BusyIndicator busyIndicator = new BusyIndicator();
        busyIndicator.setOpaque(false);
        busyIndicator.setVisible(group.isDownloading());
        busyIndicator.setPreferredSize(new Dimension(size, size));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.LINE_START);
        panel.add(busyIndicator, BorderLayout.LINE_END);
        panel.setPreferredSize(new Dimension(RIGHT_ALIGNMENT, size));
        return panel;
    }

    private static Component createSupplierComponent(SWEKSupplier supplier) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JCheckBox checkBox = new JCheckBox(supplier.getName(), supplier.isActive());
        checkBox.addActionListener(e -> supplier.activate(checkBox.isSelected()));
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.LINE_START);

        SWEKGroup group = supplier.getGroup();
        if (group.containsFilter()) {
            FilterDialog filterDialog = new FilterDialog(group, supplier);
            JideButton filterButton = new JideButton("Filter");
            filterButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point pressedLocation = e.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                    filterDialog.setVisible(true);
                }
            });
            panel.setPreferredSize(new Dimension(RIGHT_ALIGNMENT, filterButton.getPreferredSize().height));
            panel.add(filterButton, BorderLayout.LINE_END);
        }
        return panel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        tree.setEnabled(enabled);
        groupComponents.values().forEach(component -> setEnabledRecursively(component, enabled));
        supplierComponents.values().forEach(component -> setEnabledRecursively(component, enabled));
    }

    @Override
    public void removeNotify() {
        loadingTimer.stop();
        super.removeNotify();
    }

    private static void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents())
                setEnabledRecursively(child, enabled);
        }
    }

    private final class Renderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component component = componentFor(value);
            if (component != null)
                return component;
            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }

    }

    private final class Editor extends DefaultCellEditor {

        Editor() {
            super(new JCheckBox());
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row) {
            Component component = componentFor(value);
            if (component != null)
                return component;
            return super.getTreeCellEditorComponent(tree, value, selected, expanded, leaf, row);
        }

    }

}
