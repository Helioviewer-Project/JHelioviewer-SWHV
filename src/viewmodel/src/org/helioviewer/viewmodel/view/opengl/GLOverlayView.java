package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.OverlayView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;

/**
 * Implementation of OverlayView for rendering in OpenGL mode.
 * 
 * <p>
 * This class provides the capability to draw overlays in OpenGL. Therefore it
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

    private PhysicalRenderer overlayRenderer;
    private LayeredView layeredView;

    /**
     * {@inheritDoc}
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
    }

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL gl) {

        renderChild(gl);

        if (overlayRenderer != null && (layeredView == null || layeredView.getNumLayers() > 0)) {
            GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
            overlayRenderer.render(glRenderGraphics);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRenderer(PhysicalRenderer renderer) {
        overlayRenderer = renderer;
    }

    /**
     * {@inheritDoc}
     */
    public PhysicalRenderer getRenderer() {
        return overlayRenderer;
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
        }

        super.viewChanged(sender, aEvent);
    }

}
