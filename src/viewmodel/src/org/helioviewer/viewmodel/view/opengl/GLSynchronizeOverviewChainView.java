package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.SynchronizeOverviewChainView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Implementation of SynchronizeView for rendering in OpenGL2.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderGL(GL2 gl, boolean nextView) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            GLTextureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class));
        }
    }

}
