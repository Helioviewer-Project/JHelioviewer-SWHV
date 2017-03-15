package org.helioviewer.jhv.io;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;

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
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;

import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public class DataSourcesTree extends JTree {

    public static class Item {

        public final String name;
        public final String description;

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

    public DataSourcesTree() {
        nodeRoot = new DefaultMutableTreeNode("Datasets");

        for (String serverName : DataSources.getServers()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new Item(serverName, DataSources.getServerSetting(serverName, "label")));
            nodes.put(serverName, node);
            nodeRoot.add(node);
        }

        setModel(new DefaultTreeModel(nodeRoot));
        // setRootVisible(false);

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        setSelectionModel(new OneLeafTreeSelectionModel());
        ToolTipManager.sharedInstance().registerComponent(this);
        SearchableUtils.installSearchable(this).setRecursive(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path;
                if (e.getClickCount() == 2 && getRowForLocation(e.getX(), e.getY()) != -1 && (path = getPathForLocation(e.getX(), e.getY())) != null) {
                    Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (obj instanceof SourceItem)
                        ObservationDialog.getInstance().loadButtonPressed();
                }
            }
        });
    }

    @Override
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        return null; // disable builtin search
    }

    private static void reattach(DefaultMutableTreeNode tgt, DefaultMutableTreeNode src) {
        tgt.removeAllChildren();
        while (src.getChildCount() > 0)
            tgt.add((DefaultMutableTreeNode) src.getFirstChild());
    }

    public boolean setParsedData(DataSourcesParser parser) {
        String server = parser.rootNode.toString();
        for (String serverName : DataSources.getServers()) {
            if (serverName.equals(server)) {
                reattach(nodes.get(serverName), parser.rootNode);
                break;
            }
        }

        boolean preferred = server.equals(Settings.getSingletonInstance().getProperty("default.server"));
        if (preferred && parser.defaultNode != null)
            setSelectionPath(new TreePath(parser.defaultNode.getPath()));
        return preferred;
    }

    public void setSelectedItem(String server, int sourceId) {
        Enumeration<?> e = nodeRoot.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                SourceItem item = (SourceItem) node.getUserObject();
                if (item.sourceId == sourceId && item.server.equals(server)) {
                    setSelectionPath(new TreePath(node.getPath()));
                    break;
                }
            }
        }
    }

    public SourceItem getSelectedItem() {
        TreePath path = getSelectionPath();
        if (path != null) {
            Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (obj instanceof SourceItem)
                return (SourceItem) obj;
        }
        return null; // only on source load error
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        TreePath path;
        if (getRowForLocation(e.getX(), e.getY()) == -1 || (path = getPathForLocation(e.getX(), e.getY())) == null)
            return null;

        Object obj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (obj instanceof Item)
            return ((Item) obj).description;
        return null;
    }

    private static class OneLeafTreeSelectionModel extends DefaultTreeSelectionModel {

        TreePath selectedPath;

        public OneLeafTreeSelectionModel() {
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        private void setSelectionPathInternal(TreePath path) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                super.setSelectionPath(path);
                selectedPath = path;

                ObservationDialog.getInstance().setAvailabilityStatus(DataSources.getServerSetting(((SourceItem) node.getUserObject()).server, "availability.images") != null);
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
                selection = new TreePath[] { selectedPath };
        }

        @Override
        public void clearSelection() {
        }

    }

}
