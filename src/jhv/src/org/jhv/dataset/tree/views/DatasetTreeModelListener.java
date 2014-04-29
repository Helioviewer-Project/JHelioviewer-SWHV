package org.jhv.dataset.tree.views;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

public class DatasetTreeModelListener implements TreeModelListener {
    private DatasetTree tree;

    public DatasetTreeModelListener(DatasetTree tree) {
        this.tree = tree;

    }

    public void treeNodesInserted(TreeModelEvent e) {
        // first option
        tree.expandPath(new TreePath(e.getPath()));
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {

    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {

    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {

    }
}
