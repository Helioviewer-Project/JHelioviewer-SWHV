package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.AbstractBasicView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Abstract base class implementing GLView, providing some common OpenGL
 * functions.
 *
 * <p>
 * This class provides some functions common or useful for all OpenGL views,
 * including the capability to render its successor and handle the case, it the
 * successor is not a GLView.
 *
 * @author Markus Langenberg
 *
 */
public abstract class AbstractGLView extends AbstractBasicView implements GLView, ModifiableInnerViewView, ViewListener {

    protected final static GLTextureHelper textureHelper = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        notifyViewListeners(aEvent);
    }

    /**
     * Renders the successor (or child) or this view.
     *
     * This is a service function for all GLViews, so that they do not have to
     * take care whether their child is a GLView itself or not. The child is
     * always rendered in the correct way, so other GLViews may call this
     * function during their {@link #renderGL(GL)} function.
     *
     * @param gl
     *            Valid reference to the current gl object
     */
    protected void renderChild(GL2 gl) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class));
        }
    }

}
