package org.helioviewer.gl3d.view;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.view.opengl.GLView;

/**
 * The top-level Interface for 3D image Views. 3D views will only be handled
 * correctly when directly attached to another 3D view. Do not add a 3D view
 * after a 2D view!
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public interface GL3DView extends GLView {
    public void render3D(GL3DState state);

    public void deactivate(GL3DState state);
}
