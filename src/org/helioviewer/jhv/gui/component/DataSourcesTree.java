package org.helioviewer.jhv.gui.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.helioviewer.jhv.app.Settings;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.DataSourcesParser;
import org.helioviewer.jhv.io.DataSourcesParser.Item;
import org.helioviewer.jhv.io.DataSourcesParser.SourceItem;

@SuppressWarnings("serial")
public final class DataSourcesTree extends JTree {

    private final DefaultMutableTreeNode nodeRoot;
    private final HashMap<String, DefaultMutableTreeNode> nodes = new HashMap<>();

    public DataSourcesTree(Runnable activationHandler) {
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

        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        ToolTipManager.sharedInstance().registerComponent(this);
        com.jidesoft.swing.SearchableUtils.installSearchable(this).setRecursive(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getItemAt(e) instanceof SourceItem)
                    activationHandler.run();
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

    private static DefaultMutableTreeNode toTreeNode(DataSourcesParser.Node node) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node.item(), !(node.item() instanceof SourceItem));
        for (DataSourcesParser.Node child : node.children())
            treeNode.add(toTreeNode(child));
        return treeNode;
    }

    @Nullable
    public SourceItem setParsedData(DataSourcesParser parser) {
        String server = parser.getServer();
        DefaultMutableTreeNode node = nodes.get(server);
        if (node != null) {
            node.removeAllChildren();
            for (DataSourcesParser.Node child : parser.getNodes())
                node.add(toTreeNode(child));
            ((DefaultTreeModel) getModel()).nodeStructureChanged(node);
        }

        boolean preferred = server.equals(Settings.getProperty("dataSources.defaultServer"));
        if (!preferred)
            return null;

        SourceItem defaultItem = parser.getDefault();
        if (defaultItem != null)
            setSelectedItem(defaultItem.server, defaultItem.sourceId);
        return defaultItem;
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

    public List<SourceItem> getSelectedItems() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null)
            return List.of();

        List<SourceItem> items = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.getUserObject() instanceof SourceItem item)
                items.add(item);
        }
        return List.copyOf(items);
    }

    @Nullable
    @Override
    public String getToolTipText(MouseEvent e) {
        if (e == null) // may receive null according to docs
            return null;

        Item item = getItemAt(e);
        return item == null ? null : item.description;
    }

}
