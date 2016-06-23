package org.helioviewer.jhv.io;

import java.awt.event.MouseEvent;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class DataSourcesTree extends JTree {

    public DataSourcesTree() {
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);

        setSelectionModel(new OneLeafTreeSelectionModel());
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setParsedData(DataSourcesParser parser) {
        setModel(parser.model);
        if (parser.defaultPath != null)
            setSelectionPath(new TreePath(parser.defaultPath));
    }

    public DataSourcesParser.SourceItem getSelectedItem() {
        Object obj = ((DefaultMutableTreeNode) getSelectionPath().getLastPathComponent()).getUserObject();
        if (obj instanceof DataSourcesParser.SourceItem)
            return (DataSourcesParser.SourceItem) obj;
        return null; // should not happen
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (getRowForLocation(e.getX(), e.getY()) == -1)
            return null;
        Object obj = ((DefaultMutableTreeNode) getPathForLocation(e.getX(), e.getY()).getLastPathComponent()).getUserObject();
        if (obj instanceof DataSourcesParser.Item)
            return ((DataSourcesParser.Item) obj).description;
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
            if (selectedPath != null)
                selection = new TreePath[] { selectedPath };
        }

        @Override
        public void clearSelection() {
        }

    }

}
