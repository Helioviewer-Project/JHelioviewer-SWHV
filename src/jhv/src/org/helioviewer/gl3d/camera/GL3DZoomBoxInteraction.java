package org.helioviewer.gl3d.camera;

import java.awt.event.MouseEvent;

import javax.media.opengl.GL2;

import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.gl3d.GL3DState;
import org.helioviewer.jhv.display.Displayer;

/**
 * The zoom box interaction allows the user to select a region of interest in
 * the scene by dragging. The camera then moves accordingly so that only the
 * selected region is contained within the view frustum. If the zoom box is
 * restricted to the solar disk, the camera panning will be reset and a rotation
 * is applied. When the zoom box intersects with the corona the rotation is
 * reset and only a panning is applied.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DZoomBoxInteraction extends GL3DDefaultInteraction {

    private GL3DVec3d zoomBoxStartPoint;
    private GL3DVec3d zoomBoxEndPoint;

    public GL3DZoomBoxInteraction(GL3DSolarRotationTrackingTrackballCamera camera) {
        super(camera);
    }

    @Override
    public void drawInteractionFeedback(GL3DState state, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
            double x0, x1, y0, y1, z0, z1;
            if (this.zoomBoxEndPoint.x > this.zoomBoxStartPoint.x) {
                x0 = this.zoomBoxStartPoint.x;
                x1 = this.zoomBoxEndPoint.x;
                z0 = this.zoomBoxStartPoint.z;
                z1 = this.zoomBoxEndPoint.z;
            } else {
                x1 = this.zoomBoxStartPoint.x;
                x0 = this.zoomBoxEndPoint.x;
                z1 = this.zoomBoxStartPoint.z;
                z0 = this.zoomBoxEndPoint.z;
            }
            if (this.zoomBoxEndPoint.y > this.zoomBoxStartPoint.y) {
                y0 = this.zoomBoxStartPoint.y;
                y1 = this.zoomBoxEndPoint.y;
            } else {
                y1 = this.zoomBoxStartPoint.y;
                y0 = this.zoomBoxEndPoint.y;
            }

            GL2 gl = state.gl;
            gl.glColor3d(1, 1, 0);
            gl.glDisable(GL2.GL_TEXTURE_2D);

            gl.glLineWidth(2.0f);
            gl.glBegin(GL2.GL_LINE_LOOP);

            gl.glVertex3d(x0, y0, z0);
            gl.glVertex3d(x1, y0, z1);
            gl.glVertex3d(x1, y1, z1);
            gl.glVertex3d(x0, y1, z0);

            gl.glEnd();

            gl.glLineWidth(1.0f);

            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
    }

    @Override
    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxStartPoint = camera.getVectorFromSphere(e.getPoint());
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxEndPoint = camera.getVectorFromSphere(e.getPoint());
        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxEndPoint = null;
        this.zoomBoxStartPoint = null;
        Displayer.display();
    }

    private boolean isValidZoomBox() {
        return this.zoomBoxEndPoint != null && this.zoomBoxStartPoint != null;
    }

}
