package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.awt.EventQueue;
import java.util.Date;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.base.math.Interval;

public class HEKCacheTreeModel implements TreeModel, HEKCacheListener {

    HEKCache cache;
    HEKCacheModel cacheModel;

    public HEKCacheTreeModel(HEKCache cache) {
        this.cache = cache;
        this.cacheModel = cache.getModel();

        cacheModel.addCacheListener(this);
    }

    private Vector<TreeModelListener> treeModelListeners = new Vector<TreeModelListener>();

    Interval<Date> curInterval;

    /**
     * @inheritDoc
     */
    public Object getChild(Object obj, int index) {

        Object result = null;

        cache.lockRead();
        try {
            if (obj instanceof HEKPath) {
                HEKPath path = (HEKPath) obj;
                Vector<HEKPath> children = cacheModel.getChildren(path, true);
                if (index > children.size()) {
                    result = null;
                } else {
                    result = children.get(index);
                }
            }
        } finally {
            cache.unlockRead();
        }

        return result;

    }

    /**
     * @inheritDoc
     */
    public int getChildCount(Object obj) {

        int result = 0;

        cache.lockRead();

        try {

            if (obj instanceof HEKPath) {
                HEKPath path = (HEKPath) obj;
                Vector<HEKPath> children = cacheModel.getChildren(path, true);
                result = children.size();
            }

        } finally {
            cache.unlockRead();
        }

        return result;

    }

    /**
     * @inheritDoc
     */
    public int getIndexOfChild(Object parent, Object child) {

        int result = 0;

        cache.lockRead();

        try {
            if (parent instanceof HEKPath && child instanceof HEKPath) {
                HEKPath par = (HEKPath) parent;
                HEKPath chi = (HEKPath) child;
                result = cacheModel.getChildren(par, true).indexOf(chi);
            } else {
                assert (false); // should not happen
            }
        } finally {
            cache.unlockRead();
        }

        return result;

    }

    /**
     * The root of the tree is represented by this special path, pointing to the
     * cache itself
     */
    public HEKPath getRoot() {
        return new HEKPath(cache);
    }

    /**
     * @inheritDoc
     */
    public boolean isLeaf(Object obj) {

        boolean result = false;

        cache.lockRead();
        try {
            result = this.getChildCount(obj) == 0;
        } finally {
            cache.unlockRead();
        }

        return result;

    }

    public void addTreeModelListener(TreeModelListener listener) {
        treeModelListeners.add(listener);
    }

    /**
     * @inheritDoc
     */
    public void removeTreeModelListener(TreeModelListener listener) {
        treeModelListeners.remove(listener);
    }

    /**
     * Messaged when the user has altered the value for the item identified by
     * path to newValue. If newValue signifies a truly new value the model
     * should post a treeNodesChanged event.
     */
    public void valueForPathChanged(TreePath arg0, Object arg1) {
        // not an issue for us...
    }

    protected void fireTreeNodesChanged(final HEKPath path) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                for (TreeModelListener t : treeModelListeners) {
                    t.treeNodesChanged(new TreeModelEvent(this, path.getTreePath()));
                }
            }
        });
    }

    protected void fireTreeStructureChanged(final HEKPath path) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                for (TreeModelListener t : treeModelListeners) {
                    t.treeStructureChanged(new TreeModelEvent(this, path.getTreePath()));
                }
            }
        });
    }

    public void cacheStateChanged() {
    }

    public void structureChanged(HEKPath path) {
        this.fireTreeStructureChanged(path);
    }

    public void eventsChanged(HEKPath path) {
        // update all upper nodes as well
        for (int i = path.getDepth(); i > 0; i--) {
            this.fireTreeNodesChanged(path.truncate(i));
        }
    }

}
