package org.helioviewer.gl3d.view;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * The GL3DOrthoView sets the projection used in GL to a orthographic
 * projection. This is required so that the 2D sub-chain can still render with
 * an orthographic projection.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DOrthoView extends AbstractGL3DView implements GL3DView {

    private RegionView regionView;

    private float xOffset = 0.0f;
    private float yOffset = 0.0f;

    private volatile Vector2dInt mainImagePanelSize = null;

    public void setOffset(Vector2dInt offset) {
        Log.debug("GL3DOrthoView:setOffset = " + offset);
        this.xOffset = offset.getX();
        this.yOffset = offset.getY();
    }

    public void updateMainImagePanelSize(Vector2dInt size) {
        this.mainImagePanelSize = size;
        Log.debug("GL3DOrthoView:updateMainImagePanelSize = " + size);
    }

    public void render3D(GL3DState state) {
        GL gl = state.gl;

        // Viewport viewport =
        // view.getAdapter(ViewportView.class).getViewport();

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0, state.getViewportWidth(), 0, state.getViewportHeight(), -1, 10000);
        // Log.debug("GL3DOrthoView: Set Ortho Projection width="+state.getViewportWidth()+" height="+state.getViewportHeight());
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(view);
        if (viewportImageSize != null) {
            // Draw image
            gl.glPushMatrix();

            Region region = regionView.getRegion();

            float xOffsetFinal = xOffset;
            float yOffsetFinal = yOffset;

            if (mainImagePanelSize != null) {
                if (viewportImageSize.getWidth() < mainImagePanelSize.getX()) {
                    xOffsetFinal += (mainImagePanelSize.getX() - viewportImageSize.getWidth()) / 2;
                }
                if (viewportImageSize.getHeight() < mainImagePanelSize.getY()) {
                    yOffsetFinal += (mainImagePanelSize.getY() - viewportImageSize.getHeight()) / 2;
                }
            }

            Log.debug("GL3DOrthoView: Set Ortho Offset=" + xOffsetFinal + ":" + yOffsetFinal);

            // gl.glTranslatef(xOffsetFinal, state.getViewportHeight() -
            // viewportImageSize.getHeight() - yOffsetFinal, 0.0f);
            gl.glScalef(viewportImageSize.getWidth() / (float) region.getWidth(), viewportImageSize.getHeight() / (float) region.getHeight(), 1.0f);
            gl.glTranslated(-region.getCornerX(), -region.getCornerY(), 0.0);

            // Log.debug("OrthoView: region.cornerX="+region.getCornerX()+", region.cornerY="+region.getCornerY()+", viewportImageHeight="+viewportImageSize.getHeight()+", viewportImageWidth="+viewportImageSize.getWidth()+", viewport.height="+viewport.getHeight()+", viewport.width="+viewport.getWidth());

            if (view instanceof GLView) {
                ((GLView) view).renderGL(gl);
            } else {
                textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData());
            }
            gl.glPopMatrix();
        }

        // Resume Previous Projection
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (newView != null) {
            regionView = newView.getAdapter(RegionView.class);
        }
    }

    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            regionView = view.getAdapter(RegionView.class);
        }
        notifyViewListeners(aEvent);
    }
}
