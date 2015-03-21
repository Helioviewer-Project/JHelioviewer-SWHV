package org.helioviewer.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

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

}
