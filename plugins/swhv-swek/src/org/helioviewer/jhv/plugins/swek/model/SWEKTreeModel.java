package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;

public class SWEKTreeModel implements TreeModel {

    /** The event type for this model */
    private final SWEKEventType eventType;

    /** Holds the TreeModelListeners */
    private final List<TreeModelListener> listeners;

    /**
     * Creates a SWEKTreeModel for the given SWEK event type.
     * 
     * @param eventType
     *            The event type for which to create the tree model
     */
    public SWEKTreeModel(SWEKEventType eventType) {
        this.eventType = eventType;
        this.listeners = new ArrayList<TreeModelListener>();
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        this.listeners.add(l);

    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof SWEKEventType) {
            return ((SWEKEventType) parent).getSuppliers().get(index);
        } else {
            return null;
        }
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof SWEKEventType) {
            return ((SWEKEventType) parent).getSuppliers().size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if ((parent instanceof SWEKEventType) && (child instanceof SWEKSupplier)) {
            int count = 0;
            for (SWEKSupplier supplier : ((SWEKEventType) parent).getSuppliers()) {
                if (supplier.equals(child)) {
                    return count;
                } else {
                    count++;
                }
            }
        }
        return -1;
    }

    @Override
    public Object getRoot() {
        return this.eventType;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node instanceof SWEKEventType) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        this.listeners.remove(l);

    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

}
