package org.helioviewer.gl3d.plugin;

import org.helioviewer.gl3d.scenegraph.GL3DNode;

public interface GL3DModelListener {
    public void modelLoaded(GL3DNode modelRoot, GL3DModelPlugin plugin);

    public void modelUnloaded(GL3DNode modelRoot, GL3DModelPlugin plugin);
}
