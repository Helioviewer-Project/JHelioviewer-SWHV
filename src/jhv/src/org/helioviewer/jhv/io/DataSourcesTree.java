package org.helioviewer.jhv.io;

import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class DataSourcesTree extends JTree {

    public static class Item {

        public final String key;
        public final String name;
        public final String description;

        public Item(String key, String name, String description) {
            this.key = key;
            this.name = name;
            this.description = description;
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

        public SourceItem(String server, String key, String name, String description, int sourceId, long start, long end, boolean defaultItem) {
            super(key, name, description);
            this.server = server;
            this.sourceId = sourceId;
            this.start = start;
            this.end = end;
            this.defaultItem = defaultItem;
        }

    }

    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode nodeROB;
    private final DefaultMutableTreeNode nodeGSFC;
    private final DefaultMutableTreeNode nodeIAS;

    public DataSourcesTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("DataSources");
        nodeROB = new DefaultMutableTreeNode(new Item("ROB", "ROB", "Royal Observatory of Belgium"));
        nodeGSFC = new DefaultMutableTreeNode(new Item("GSFC", "GSFC", "Goddard Space Flight Center"));
        nodeIAS = new DefaultMutableTreeNode(new Item("IAS", "IAS", "Institut d'Astrophysique Spatiale"));
        root.add(nodeROB);
        root.add(nodeGSFC);
        root.add(nodeIAS);

        treeModel = new DefaultTreeModel(root);
        setModel(treeModel);
        setRootVisible(false);

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        setSelectionModel(new OneLeafTreeSelectionModel());
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    private void reattach(DefaultMutableTreeNode tgt, DefaultMutableTreeNode src) {
        tgt.removeAllChildren();
        while (src.getChildCount() > 0)
            tgt.add((DefaultMutableTreeNode) src.getFirstChild());
    }

    public void setParsedData(DataSourcesParser parser) {
        String server = parser.rootNode.toString();
        if ("ROB".equals(server))
            reattach(nodeROB, parser.rootNode);
        else if ("GSFC".equals(server))
            reattach(nodeGSFC, parser.rootNode);
        else
            reattach(nodeIAS, parser.rootNode);

        if (parser.defaultNode != null)
            setSelectionPath(new TreePath(parser.defaultNode.getPath()));
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
        if (getRowForLocation(e.getX(), e.getY()) == -1)
            return null;
        Object obj = ((DefaultMutableTreeNode) getPathForLocation(e.getX(), e.getY()).getLastPathComponent()).getUserObject();
        if (obj instanceof Item)
            return ((Item) obj).description;
        return null;
    }

    private static class OneLeafTreeSelectionModel extends DefaultTreeSelectionModel {

        TreePath selectedPath;

        public OneLeafTreeSelectionModel() {
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        @Override
        public void setSelectionPath(TreePath path) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                super.setSelectionPath(path);
                selectedPath = path;
            }
        }

        @Override
        public void addSelectionPath(TreePath path) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node.isLeaf() && node.getUserObject() instanceof SourceItem) {
                super.addSelectionPath(path);
                selectedPath = path;
            }
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
