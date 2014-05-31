package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.display.Displayer;

/**
 * This interaction is used by the {@link GL3DTrackballCamera} as its rotation
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
    private volatile GL3DQuatd currentDragRotation;

    protected GL3DTrackballRotationInteraction(GL3DSolarRotationTrackingTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.currentRotationEndPoint = getVectorFromSphere(e.getPoint(), camera);

        currentDragRotation = GL3DQuatd.calcRotation(currentRotationStartPoint, currentRotationEndPoint);
        camera.rotateCurrentDragRotation(currentDragRotation);
        this.camera.updateCameraTransformation(false);

        camera.fireCameraMoving();
        Displayer.getSingletonInstance().display();

    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        this.currentRotationStartPoint = null;
        this.currentRotationEndPoint = null;

        camera.fireCameraMoved();
        Displayer.getSingletonInstance().display();

    }

    @Override
    public void reset() {
        if (this.currentDragRotation != null) {
            this.currentDragRotation.clear();
        }
        super.reset();
    }

    @Override
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
            hitPoint = ray.getHitPoint();
            hitPoint.normalize();

            hitPoint = camera.getLocalRotation().toMatrix().multiply(hitPoint);
            double radeg = 180. / Math.PI;

        } else {
            double y = (camera.getHeight() / 2 - p.y) / camera.getHeight();
            double x = (p.x - camera.getWidth() / 2) / camera.getWidth();

            GL3DVec3d nv = new GL3DVec3d(x, y, 0);
            nv.normalize();
            hitPoint = camera.getRotation().toMatrix().inverse().multiply(nv);
            hitPoint = camera.getLocalRotation().toMatrix().multiply(hitPoint);
            /*
             * //
             * Log.debug("GL3DTrackballRotationInteraction: Ray is Outside!");
             * System.out.println(ray.getOrigin() + " " + ray.getDirection());
             * GL3DVec3d vec = ray.getDirection().copy();
             * vec.multiply(ray.getOrigin().length()); vec.add(ray.getOrigin());
             * vec.multiply(-1.); // vec.z = -Math.abs(vec.z) * vec.z; hitPoint
             * = camera.getLocalRotation().toMatrix().multiply(vec);
             * System.out.println(vec + "HP " + hitPoint);
             */
        }
        return hitPoint;
    }
}
