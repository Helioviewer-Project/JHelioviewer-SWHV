package org.helioviewer.gl3d.view;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.view.opengl.AbstractGLView;

/**
 * Default super class for all {@link GL3DView}s. Provides default behavior like
 * view chain traversal.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class AbstractGL3DView extends AbstractGLView implements GL3DView {

    public void renderGL(GL gl) {
        render3D(GL3DState.get());
    }

    public void deactivate(GL3DState state) {
        if (getView() != null) {
            if (getView().getAdapter(GL3DView.class) != null) {
                getView().getAdapter(GL3DView.class).deactivate(state);
            }
        }

    }
}
