package org.helioviewer.jhv.plugins.swek.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

import org.helioviewer.jhv.event.SWEKGroup;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.gui.component.BusyIndicator;
import org.helioviewer.jhv.plugins.swek.SWEKIconImages;

import com.formdev.flatlaf.util.ScaledImageIcon;
import com.formdev.flatlaf.util.UIScale;
import com.jidesoft.swing.JideButton;

@SuppressWarnings("serial")
final class SWEKTreePane extends JPanel {

    private static final int RIGHT_ALIGNMENT = 300;

    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final IdentityHashMap<SWEKGroup, Component> groupComponents = new IdentityHashMap<>();
    private final IdentityHashMap<SWEKGroup, DefaultMutableTreeNode> groupNodes = new IdentityHashMap<>();
    private final IdentityHashMap<SWEKSupplier, Component> supplierComponents = new IdentityHashMap<>();
    private final SWEKSession session;
    private final SWEKSwingUI ui;
    private final Timer loadingTimer;

    SWEKTreePane(SWEKSession _session, SWEKSwingUI _ui) {
        super(new BorderLayout());
        session = _session;
        ui = _ui;
        treeModel = createTreeModel(session.catalog().getGroups());

        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setEditable(true);
        tree.setShowsRootHandles(true);
        tree.setSelectionModel(null);
        tree.setCellRenderer(new Renderer());
        tree.setCellEditor(new Editor());
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);

        loadingTimer = new Timer(500, e -> repaintBusyGroups());

        setBorder(BorderFactory.createEmptyBorder());
        add(tree, BorderLayout.CENTER);
    }

    private DefaultTreeModel createTreeModel(List<SWEKGroup> groups) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        DefaultTreeModel model = new DefaultTreeModel(root);
        for (SWEKGroup group : groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            groupNodes.put(group, groupNode);
            for (SWEKSupplier supplier : session.catalog().getSuppliers(group)) {
                groupNode.add(new DefaultMutableTreeNode(supplier));
            }
            root.add(groupNode);
        }
        return model;
    }

    private void groupBusyChanged(SWEKGroup group) {
        DefaultMutableTreeNode groupNode = groupNodes.get(group);
        if (groupNode != null)
            treeModel.nodeChanged(groupNode);
    }

    private void repaintBusyGroups() {
        boolean anyBusy = false;
        for (Map.Entry<SWEKGroup, DefaultMutableTreeNode> entry : groupNodes.entrySet()) {
            if (session.isGroupBusy(entry.getKey())) {
                anyBusy = true;
                repaintGroup(entry.getValue());
            }
        }

        if (!anyBusy)
            loadingTimer.stop();
    }

    private void repaintGroup(DefaultMutableTreeNode groupNode) {
        Rectangle bounds = tree.getPathBounds(new TreePath(groupNode.getPath()));
        if (bounds != null)
            tree.repaint(bounds);
    }

    private Component componentFor(Object value) {
        Component component = null;
        if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof SWEKGroup group) {
            boolean busy = session.isGroupBusy(group);
            if (busy && !loadingTimer.isRunning())
                loadingTimer.start();
            component = groupComponents.computeIfAbsent(group, this::createGroupComponent);
            if (component instanceof JPanel panel && panel.getComponentCount() > 1)
                panel.getComponent(1).setVisible(busy);
        } else if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof SWEKSupplier supplier) {
            component = supplierComponents.computeIfAbsent(supplier, this::createSupplierComponent);
            if (component instanceof JPanel panel && panel.getComponent(0) instanceof JCheckBox checkBox)
                checkBox.setSelected(session.isSupplierActive(supplier));
        }

        if (component != null)
            setEnabledRecursively(component, tree.isEnabled());
        return component;
    }

    private Component createGroupComponent(SWEKGroup group) {
        JLabel label = new JLabel(group.getName());
        int size = label.getPreferredSize().height;
        int iconSize = UIScale.unscale(size);
        label.setIcon(new ScaledImageIcon(new ImageIcon(SWEKIconImages.getImage(group.getIconKey())), iconSize, iconSize));
        size = label.getPreferredSize().height;

        BusyIndicator busyIndicator = new BusyIndicator();
        busyIndicator.setOpaque(false);
        busyIndicator.setVisible(session.isGroupBusy(group));
        busyIndicator.setPreferredSize(new Dimension(size, size));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(label, BorderLayout.LINE_START);
        panel.add(busyIndicator, BorderLayout.LINE_END);
        panel.setPreferredSize(new Dimension(RIGHT_ALIGNMENT, size));
        return panel;
    }

    private Component createSupplierComponent(SWEKSupplier supplier) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JCheckBox checkBox = new JCheckBox(supplier.displayName(), session.isSupplierActive(supplier));
        checkBox.addActionListener(e -> session.setSupplierActive(supplier, checkBox.isSelected()));
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        panel.add(checkBox, BorderLayout.LINE_START);

        if (supplier.containsFilter()) {
            JideButton filterButton = new JideButton("Filter");
            filterButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point pressedLocation = e.getLocationOnScreen();
                    ui.showFilter(supplier, pressedLocation);
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
    public void addNotify() {
        super.addNotify();
        session.setGroupChangedCallback(this::groupBusyChanged);
    }

    @Override
    public void removeNotify() {
        session.setGroupChangedCallback(_ -> {});
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
