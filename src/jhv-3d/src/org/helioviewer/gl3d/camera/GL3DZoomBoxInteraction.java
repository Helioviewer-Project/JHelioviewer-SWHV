package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DBaseTrackballCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.jhv.display.Displayer;

/**
 * The zoom box interaction allows the user to select a region of interest in
 * the scene by dragging. The camera then moves accordingly so that only the
 * selected region is contained within the view frustum. If the zoom box is
 * restricted to the solar disk, the camera panning will be reset and a rotation
 * is applied. When the zoom box intersects with the corona the rotation is
 * reset and only a panning is applied.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomBoxInteraction extends GL3DDefaultInteraction {
    private GL3DRayTracer rayTracer;
    private GL3DVec3d zoomBoxStartPoint;
    private GL3DVec3d zoomBoxEndPoint;

    private SolarImageCoordinateSystem solarDiskCS;

    public GL3DZoomBoxInteraction(GL3DBaseTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
        this.solarDiskCS = new SolarImageCoordinateSystem();
    }

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

            GL gl = state.gl;
            gl.glColor3d(1, 1, 0);
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_TEXTURE_2D);

            gl.glLineWidth(2.0f);
            gl.glBegin(GL.GL_LINE_LOOP);

            gl.glVertex3d(x0, y0, z0);
            gl.glVertex3d(x1, y0, z1);
            gl.glVertex3d(x1, y1, z1);
            gl.glVertex3d(x0, y1, z0);

            gl.glEnd();

            gl.glLineWidth(1.0f);
            gl.glEnable(GL.GL_LIGHTING);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glEnable(GL.GL_TEXTURE_2D);
        }
    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxStartPoint = getHitPoint(e.getPoint());
        Displayer.getSingletonInstance().display();
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxEndPoint = getHitPoint(e.getPoint());
        Displayer.getSingletonInstance().display();
    }

    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
            camera.addCameraAnimation(createZoomAnimation());

            if (isCompletelyOnSphere()) {
                if (camera.getTranslation().x != 0 || camera.getTranslation().y != 0) {
                    // Reset Panning
                    camera.addCameraAnimation(createPanAnimation(0, 0));
                    // Rotate from middle point
                    camera.addCameraAnimation(createRotationAnimation(new GL3DVec3d(0, 0, 1)));
                } else {
                    GL3DVec3d hitPoint = getHitPoint(new Point((int) this.camera.getWidth() / 2, (int) this.camera.getHeight() / 2));
                    if (hitPoint != null) {
                        GL3DVec3d startPoint = hitPoint.normalize();
                        camera.addCameraAnimation(createRotationAnimation(startPoint));
                    } else {
                        Log.error("GL3DZoomBoxInteraction: No Hitpoint returned on Sphere!");
                    }
                }
            } else {
                long x = Math.round(-(this.zoomBoxEndPoint.x + this.zoomBoxStartPoint.x) / 2);
                long y = Math.round(-(this.zoomBoxEndPoint.y + this.zoomBoxStartPoint.y) / 2);
                camera.addCameraAnimation(createPanAnimation(x, y));
            }
        }
        this.zoomBoxEndPoint = null;
        this.zoomBoxStartPoint = null;
        Displayer.getSingletonInstance().display();

    }

    private GL3DCameraRotationAnimation createRotationAnimation(GL3DVec3d startPoint) {
        GL3DVec3d endPoint = GL3DVec3d.add(this.zoomBoxEndPoint, this.zoomBoxStartPoint).divide(2).normalize();

        return new GL3DCameraRotationAnimation(startPoint, endPoint, 700);
    }

    private GL3DCameraPanAnimation createPanAnimation(long x, long y) {
        GL3DVec3d distanceToMove = new GL3DVec3d((x - camera.translation.x), (y - camera.translation.y), 0);
        // Log.debug("GL3DZoomBoxInteraction: Panning "+distanceToMove);
        return new GL3DCameraPanAnimation(distanceToMove);
    }

    private GL3DCameraZoomAnimation createZoomAnimation() {
        double halfWidth = Math.abs(this.zoomBoxEndPoint.x - this.zoomBoxStartPoint.x) / 2;
        double halfFOVRad = Math.toRadians(camera.getFOV() / 2);
        double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
        distance = -distance - camera.getZTranslation();

        return new GL3DCameraZoomAnimation(distance, 700);
    }

    private boolean isCompletelyOnSphere() {
        CoordinateVector startCoord = solarDiskCS.createCoordinateVector(this.zoomBoxStartPoint.x, this.zoomBoxStartPoint.y);
        CoordinateVector endCoord = solarDiskCS.createCoordinateVector(this.zoomBoxEndPoint.x, this.zoomBoxEndPoint.y);
        return (solarDiskCS.isInsideDisc(startCoord) && solarDiskCS.isInsideDisc(endCoord));
    }

    private boolean isValidZoomBox() {
        return this.zoomBoxEndPoint != null && this.zoomBoxStartPoint != null;
    }

    protected GL3DVec3d getHitPoint(Point p) {
        this.rayTracer = new GL3DRayTracer(sceneGraphView.getHitReferenceShape(), this.camera);
        GL3DRay ray = this.rayTracer.cast(p.x, p.y);
        GL3DVec3d hitPoint = ray.getHitPoint();
        return hitPoint;
    }
}
