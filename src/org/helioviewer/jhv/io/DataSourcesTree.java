package org.helioviewer.jhv.io;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.app.Settings;

@SuppressWarnings("serial")
public final class DataSourcesTree extends JTree {

    public static class Item {

        final String name;
        final String description;

        public Item(String _name, String _description) {
            name = _name;
            description = _description;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static class SourceItem extends Item {

        public final String server;
        public final int sourceId;
        public final long start;
        public final long end;

        public SourceItem(String _server, String _name, String _description, int _sourceId, long _start, long _end) {
            super(_name, _description);
            server = _server;
            sourceId = _sourceId;
            start = _start;
            end = _end;
        }

    }

    private final DefaultMutableTreeNode nodeRoot;
    private final HashMap<String, DefaultMutableTreeNode> nodes = new HashMap<>();

    public DataSourcesTree(Consumer<SourceItem> activationHandler) {
        nodeRoot = new DefaultMutableTreeNode("Datasets");

        for (String serverName : DataSources.getServers()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Item(serverName, DataSources.getServer(serverName).label()));
            nodes.put(serverName, node);
            nodeRoot.add(node);
        }

        setModel(new DefaultTreeModel(nodeRoot));
        setRootVisible(false);
        setShowsRootHandles(true);

        if (getCellRenderer() instanceof DefaultTreeCellRenderer defaultRenderer) {
            defaultRenderer.setOpenIcon(null);
            defaultRenderer.setClosedIcon(null);
            defaultRenderer.setLeafIcon(null);
        }

        setSelectionModel(new OneLeafTreeSelectionModel());
        ToolTipManager.sharedInstance().registerComponent(this);
        com.jidesoft.swing.SearchableUtils.installSearchable(this).setRecursive(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getItemAt(e) instanceof SourceItem item)
                    activationHandler.accept(item);
            }
        });
    }

    @Nullable
    private Item getItemAt(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null && ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof Item item)
            return item;
        return null;
    }

    @Nullable
    @Override
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        return null; // disable builtin search
    }

    private static void reattach(DefaultMutableTreeNode tgt, DefaultMutableTreeNode src) {
        tgt.removeAllChildren();
        while (src.getChildCount() > 0)
            tgt.add((DefaultMutableTreeNode) src.getFirstChild());
    }

    @Nullable
    public SourceItem setParsedData(DataSourcesParser parser) {
        String server = parser.getRoot().toString();
        DefaultMutableTreeNode node = nodes.get(server);
        if (node != null) {
            reattach(node, parser.getRoot());
            ((DefaultTreeModel) getModel()).nodeStructureChanged(node);
        }

        boolean preferred = server.equals(Settings.getProperty("dataSources.defaultServer"));
        if (!preferred)
            return null;

        SourceItem defaultItem = parser.getDefault();
        if (defaultItem != null)
            setSelectedItem(defaultItem.server, defaultItem.sourceId);
        return getSelectedItem();
    }

    public void setSelectedItem(String server, int sourceId) {
        Enumeration<?> e = nodeRoot.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem item) {
                if (item.sourceId == sourceId && item.server.equals(server)) {
                    setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
        }
    }

    @Nullable
    public SourceItem getSelectedItem() {
        Object obj = getLastSelectedPathComponent();
        if (obj instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof SourceItem item)
            return item;
        return null; // only on source load error
    }

    @Nullable
    @Override
    public String getToolTipText(MouseEvent e) {
        if (e == null) // may receive null according to docs
            return null;

        Item item = getItemAt(e);
        return item == null ? null : item.description;
    }

    private static class OneLeafTreeSelectionModel extends DefaultTreeSelectionModel {

        private TreePath selectedPath;

        OneLeafTreeSelectionModel() {
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        private void setSelectionPathInternal(@Nonnull TreePath path) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                super.setSelectionPath(path);
                selectedPath = path;
            }
        }

        @Override
        public void setSelectionPath(TreePath path) {
            if (path == null)
                return;
            setSelectionPathInternal(path);
        }

        @Override
        public void addSelectionPath(TreePath path) {
            if (path == null)
                return;
            setSelectionPathInternal(path);
        }

        @Override
        public void resetRowSelection() {
            super.resetRowSelection();
            if (selectedPath != null && selection == null)
                selection = new TreePath[]{selectedPath};
        }

        @Override
        public void clearSelection() {}

    }

}
