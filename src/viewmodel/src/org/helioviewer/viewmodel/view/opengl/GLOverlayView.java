package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.View;

/**
 * Implementation of OverlayView for rendering in OpenGL mode.
 *
 * <p>
 * This class provides the capability to draw overlays in OpenGL2. Therefore it
 * manages a {@link PhysicalRenderer}, which is passed to the registered
 * renderer.
 *
 * <p>
 * For further information about the role of the OverlayView within the view
 * chain, see {@link org.helioviewer.viewmodel.view.OverlayView}.
 *
 * @author Markus Langenberg
 */
public class GLOverlayView extends AbstractGLView implements OverlayView {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        renderChild(gl);
    }

}
