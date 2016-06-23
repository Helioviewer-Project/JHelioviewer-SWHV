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

        public final int sourceId;
        public final long start;
        public final long end;
        public final boolean defaultItem;

        public SourceItem(String key, String name, String description, int sourceId, long start, long end, boolean defaultItem) {
            super(key, name, description);
            this.sourceId = sourceId;
            this.start = start;
            this.end = end;
            this.defaultItem = defaultItem;
        }

    }

    public DataSourcesTree() {
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        setSelectionModel(new OneLeafTreeSelectionModel());
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setParsedData(DataSourcesParser parser) {
        setModel(new DefaultTreeModel(parser.rootNode));
        if (parser.defaultPath != null)
            setSelectionPath(new TreePath(parser.defaultPath));
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
            if (((DefaultMutableTreeNode) path.getLastPathComponent()).isLeaf()) {
                super.setSelectionPath(path);
                selectedPath = path;
            }
        }

        @Override
        public void addSelectionPath(TreePath path) {
            if (((DefaultMutableTreeNode) path.getLastPathComponent()).isLeaf()) {
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
