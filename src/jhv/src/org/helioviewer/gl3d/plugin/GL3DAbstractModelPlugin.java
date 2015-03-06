package org.helioviewer.gl3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DNode;

public abstract class GL3DAbstractModelPlugin implements GL3DModelPlugin {
    private boolean active = true;;

    private List<GL3DModelListener> listeners = new ArrayList<GL3DModelListener>();

    private GL3DGroup root;

    public GL3DAbstractModelPlugin() {
        this.root = new GL3DGroup(getPluginName());
    }

    public GL3DGroup getPluginRootNode() {
        return this.root;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void addModelListener(GL3DModelListener listener) {
        this.listeners.add(listener);
    }

    public void removeModelListener(GL3DModelListener listener) {
        this.listeners.remove(listener);
    }

    public void fireModelLoaded(GL3DNode modelRoot) {
        for (GL3DModelListener l : this.listeners) {
            l.modelLoaded(modelRoot, this);
        }
    }

    public void fireModelUnloaded(GL3DNode modelRoot) {
        for (GL3DModelListener l : this.listeners) {
            l.modelUnloaded(modelRoot, this);
        }
    }

    public void unload() {
        this.listeners.clear();
    }
}
