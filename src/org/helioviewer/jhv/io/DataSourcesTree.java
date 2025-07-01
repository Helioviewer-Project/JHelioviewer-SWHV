package org.helioviewer.jhv.io;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;

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

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.Interfaces;

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
        public final boolean defaultItem;

        public SourceItem(String _server, String _name, String _description, int _sourceId, long _start, long _end, boolean _defaultItem) {
            super(_name, _description);
            server = _server;
            sourceId = _sourceId;
            start = _start;
            end = _end;
            defaultItem = _defaultItem;
        }

    }

    private final DefaultMutableTreeNode nodeRoot;
    private final HashMap<String, DefaultMutableTreeNode> nodes = new HashMap<>();

    public DataSourcesTree(Interfaces.ObservationSelector selector) {
        nodeRoot = new DefaultMutableTreeNode("Datasets");

        for (String serverName : DataSources.getServers()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Item(serverName, DataSources.getServerSetting(serverName, "label")));
            nodes.put(serverName, node);
            nodeRoot.add(node);
        }

        setModel(new DefaultTreeModel(nodeRoot));
        // setRootVisible(false);

        if (getCellRenderer() instanceof DefaultTreeCellRenderer defaultRenderer) {
            defaultRenderer.setOpenIcon(null);
            defaultRenderer.setClosedIcon(null);
            defaultRenderer.setLeafIcon(null);
        }

        setSelectionModel(new OneLeafTreeSelectionModel(selector));
        ToolTipManager.sharedInstance().registerComponent(this);
        com.jidesoft.swing.SearchableUtils.installSearchable(this).setRecursive(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path;
                if (e.getClickCount() == 2 && getRowForLocation(e.getX(), e.getY()) != -1 && (path = getPathForLocation(e.getX(), e.getY())) != null) {
                    Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (obj instanceof SourceItem si)
                        selector.load(si.server, si.sourceId);
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SourceItem item = getSelectedItem();
                    if (item != null)
                        selector.load(item.server, item.sourceId);
                }
            }
        });
    }

    @Nullable
    @Override
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        return null; // disable builtin search
    }

    private static DefaultMutableTreeNode copyNode(DefaultMutableTreeNode src) {
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(src.getUserObject());
        if (src.isLeaf()) {
            return copy;
        } else {
            int cc = src.getChildCount();
            for (int i = 0; i < cc; i++) {
                copy.add(copyNode((DefaultMutableTreeNode) src.getChildAt(i)));
            }
            return copy;
        }
    }

    private static void reattach(DefaultMutableTreeNode tgt, DefaultMutableTreeNode src) {
        tgt.removeAllChildren();
        Enumeration<?> children = src.children();
        while (children.hasMoreElements()) {
            tgt.add(copyNode((DefaultMutableTreeNode) children.nextElement()));
        }
    }

    public boolean setParsedData(DataSourcesParser parser) {
        String server = parser.getRoot().toString();
        for (String serverName : DataSources.getServers()) {
            if (serverName.equals(server)) {
                reattach(nodes.get(serverName), parser.getRoot());
                break;
            }
        }

        boolean preferred = server.equals(Settings.getProperty("default.server"));
        if (preferred && parser.getDefault() != null) {
            Object obj = parser.getDefault().getUserObject();
            if (obj instanceof SourceItem si) {
                setSelectedItem(si.server, si.sourceId);
            }
        }
        return preferred;
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
        TreePath path = getSelectionPath();
        if (path != null) {
            Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (obj instanceof SourceItem si)
                return si;
        }
        return null; // only on source load error
    }

    @Nullable
    @Override
    public String getToolTipText(MouseEvent e) {
        if (e == null) // may receive null according to docs
            return null;

        TreePath path;
        if (getRowForLocation(e.getX(), e.getY()) == -1 || (path = getPathForLocation(e.getX(), e.getY())) == null)
            return null;

        Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (obj instanceof Item item)
            return item.description;
        return null;
    }

    private static class OneLeafTreeSelectionModel extends DefaultTreeSelectionModel {

        private final Interfaces.ObservationSelector selector;
        private TreePath selectedPath;

        OneLeafTreeSelectionModel(Interfaces.ObservationSelector _selector) {
            selector = _selector;
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        private void setSelectionPathInternal(TreePath path) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                super.setSelectionPath(path);
                selectedPath = path;
                selector.setAvailabilityEnabled(DataSources.getServerSetting(((SourceItem) node.getUserObject()).server, "availability.images") != null);
            }
        }

        @Override
        public void setSelectionPath(TreePath path) {
            setSelectionPathInternal(path);
        }

        @Override
        public void addSelectionPath(TreePath path) {
            setSelectionPathInternal(path);
        }

        @Override
        public void resetRowSelection() {
            super.resetRowSelection();
            if (selectedPath != null && selection == null)
                selection = new TreePath[]{selectedPath};
        }

        @Override
        public void clearSelection() {
        }

    }

}
