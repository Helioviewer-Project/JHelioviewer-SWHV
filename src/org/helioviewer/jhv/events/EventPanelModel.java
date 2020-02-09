package org.helioviewer.jhv.events;

import javax.annotation.Nullable;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class EventPanelModel implements TreeModel {

    private final SWEKGroup group;

    EventPanelModel(SWEKGroup _group) {
        group = _group;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

    @Nullable
    @Override
    public Object getChild(Object parent, int index) {
        return parent instanceof SWEKGroup ? ((SWEKGroup) parent).getSuppliers().get(index) : null;
    }

    @Override
    public int getChildCount(Object parent) {
        return parent instanceof SWEKGroup ? ((SWEKGroup) parent).getSuppliers().size() : 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if ((parent instanceof SWEKGroup) && (child instanceof SWEKSupplier)) {
            return ((SWEKGroup) parent).getSuppliers().indexOf(child);
        }
        return -1;
    }

    @Override
    public Object getRoot() {
        return group;
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof SWEKSupplier;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

}
