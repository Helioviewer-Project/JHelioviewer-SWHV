package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class GL3DImageTextureView extends AbstractGL3DView implements GL3DView {

    @Override
    public void renderGL(GL2 gl, boolean nextView) {
    }

    @Override
    public void render3D(GL3DState state) {

    }

    @Override
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {

    }

    @Override
    protected void renderChild(GL2 gl) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            GLTextureHelper.renderImageDataToScreen(gl, view.getAdapter(SubimageDataView.class).getSubimageData(), view.getAdapter(JHVJP2View.class).tex);
        }
    }

    @Override
    protected void notifyViewListeners(ChangeEvent aEvent) {
        for (ViewListener v : listeners) {
            v.viewChanged(this, aEvent);
        }
    }
}
