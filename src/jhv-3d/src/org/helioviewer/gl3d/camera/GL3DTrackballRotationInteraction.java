package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DComponentView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.display.Displayer;

/**
 * This interaction is used by the {@link GL3DBaseTrackballCamera} as its rotation
 * interaction. The calculation of the rotation done by creating a rotation
 * Quaternion between two points on a sphere. These points are retrieved by
 * using the raycasting mechanism provided by {@link GL3DRayTracer}.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballRotationInteraction extends GL3DDefaultInteraction {
    private GL3DVec3d currentRotationStartPoint;
    private GL3DVec3d currentRotationEndPoint;
    private volatile GL3DQuatd currentDragRotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0.0,0.0,1.0));

    protected GL3DTrackballRotationInteraction(GL3DBaseTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
        camera.currentDragRotation = GL3DQuatd.createRotation(0.0, new GL3DVec3d(0.0,0.0,1.0));
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.currentRotationEndPoint = getVectorFromSphere(e.getPoint(), camera);
        try {
            currentDragRotation = GL3DQuatd.calcRotation(currentRotationStartPoint, currentRotationEndPoint);
            camera.currentDragRotation.rotate(currentDragRotation);
            camera.rotateAll();
        	Displayer.getSingletonInstance().display();
        } catch (IllegalArgumentException exc) {
            Log.warn("GL3DTrackballCamera.mouseDragged: Illegal Rotation ignored!", exc);
        }

        //camera.fireCameraMoving();
    	Displayer.getSingletonInstance().display();

    }

    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        this.currentRotationStartPoint = null;
        this.currentRotationEndPoint = null;

        //camera.fireCameraMoved();
    	Displayer.getSingletonInstance().display();

    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        // The start point of the rotation remains the same during a drag,
        // because the
        // mouse should always point to the same Point on the Surface of the
        // sphere.
        this.currentRotationStartPoint = getVectorFromSphere(e.getPoint(), camera);
    }

    protected GL3DVec3d getVectorFromSphere(Point p, GL3DCamera camera) {
        GL3DRayTracer sunTracer = new GL3DRayTracer(sceneGraphView.getHitReferenceShape(), camera);
        GL3DRay ray = sunTracer.cast(p.x, p.y);

        GL3DVec3d hitPoint;

        if (ray.isOnSun) {
            // Log.debug("GL3DTrackballRotationInteraction: Ray is Inside!");
            hitPoint = ray.getHitPoint();
            hitPoint = hitPoint.normalize();
        } else {
            // Log.debug("GL3DTrackballRotationInteraction: Ray is Outside!");
            double y = (camera.getHeight() / 2 - p.y) / camera.getHeight();
            double x = (p.x - camera.getWidth() / 2) / camera.getWidth();

            hitPoint = camera.getRotation().toMatrix().inverse().multiply(new GL3DVec3d(x, y, 0).normalize());
        }
        return hitPoint;
    }
}
