package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * This class sets the GL viewport to the size of the underlying viewport, which
 * in effect will be the entire size of the canvas because the size of the
 * underlying viewport is in turn adjusted by the {@link GL3DComponentView}s
 * updateImagePanelSize() method.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DViewportView extends AbstractGL3DView implements GL3DView {

    private ViewportView viewportView;

    public void render3D(GL3DState state) {
        GL2 gl = state.gl;
        Viewport viewport;

        if (viewportView != null && (viewport = viewportView.getViewport()) != null) {
            gl.glViewport(0, 0, viewport.getWidth(), viewport.getHeight());
        }
        if (this.getView() != null) {
            this.renderChild(gl);
        }
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        viewportView = newView.getAdapter(ViewportView.class);
    }

}
