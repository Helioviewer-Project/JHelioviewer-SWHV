package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL;

import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SubimageDataView;

/**
 * Extension of StandardSolarRotationTrackingView to enable tracking in OpenGL
 * mode.
 * 
 * @author Markus Langenberg
 */
public class GLSolarRotationTrackingView extends StandardSolarRotationTrackingView implements GLView {

    protected final static GLTextureHelper textureHelper = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL gl) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl);
        } else {
            textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData());
        }
    }
}
