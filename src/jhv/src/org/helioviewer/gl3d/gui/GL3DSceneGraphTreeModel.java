package org.helioviewer.gl3d.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * Currently not in use!
 * 
 * Wraps the Scene managed by the {@link GL3DSceneGraphView} in a
 * {@link TreeModel}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSceneGraphTreeModel implements TreeModel {
    private GL3DSceneGraphView sceneModelView;

    private List<TreeModelListener> listeners;

    public GL3DSceneGraphTreeModel() {
        this.listeners = new ArrayList<TreeModelListener>();
    }

    public Object getChild(Object parent, int index) {
        GL3DGroup node = (GL3DGroup) parent;
        return node.getChild(index);
    }

    public int getChildCount(Object parent) {
        GL3DGroup node = (GL3DGroup) parent;
        return node.numChildNodes();
    }

    public int getIndexOfChild(Object parent, Object child) {
        GL3DGroup p = (GL3DGroup) parent;
        GL3DGroup node = (GL3DGroup) child;
        return p.indexOfChild(node);
    }

    public Object getRoot() {
        return getSceneModelView().getRoot();
    }

    public boolean isLeaf(Object node) {
        return !(node instanceof GL3DGroup);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        // Log.debug("ValueForPathChanged!");
    }

    public void removeTreeModelListener(TreeModelListener l) {
        this.listeners.remove(l);
    }

    public void addTreeModelListener(TreeModelListener l) {
        this.listeners.add(l);
    }

    protected void fireChangeEvent() {
        for (TreeModelListener listener : this.listeners) {
            listener.treeStructureChanged(new TreeModelEvent(getRoot(), new TreePath(getRoot())));
        }
    }

    private GL3DSceneGraphView getSceneModelView() {
        if (sceneModelView == null) {
            ImageViewerGui imageViewer = ImageViewerGui.getSingletonInstance();
            if (imageViewer == null)
                return null;
            ComponentView mainView = imageViewer.getMainView();
            if (mainView == null)
                return null;

            this.sceneModelView = mainView.getAdapter(GL3DSceneGraphView.class);
        }

        return sceneModelView;
    }
}
