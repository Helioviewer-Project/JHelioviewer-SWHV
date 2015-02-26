package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Extension of StandardSolarRotationTrackingView to enable tracking in OpenGL
 * mode.
 * 
 * @author Markus Langenberg
 */
public class GLSolarRotationTrackingView extends StandardSolarRotationTrackingView implements GLView {

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL2 gl, boolean nextView) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            GLTextureHelper.renderImageDataToScreen(gl, view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class).tex);
        }
    }
}
