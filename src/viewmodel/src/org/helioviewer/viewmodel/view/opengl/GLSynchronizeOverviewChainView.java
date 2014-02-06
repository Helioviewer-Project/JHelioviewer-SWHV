package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.SynchronizeOverviewChainView;

/**
 * Implementation of SynchronizeView for rendering in OpenGL.
 * 
 * <p>
 * This class behaves exactly like
 * {@link org.helioviewer.viewmodel.view.SynchronizeOverviewChainView} extended
 * by propagating the renderGL call to its successor.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLSynchronizeOverviewChainView extends SynchronizeOverviewChainView implements GLView {

    private GLTextureHelper textureHelper = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL gl, boolean nextView) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData());
        }
    }

}
