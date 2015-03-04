package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * This interaction is used by the {@link GL3DEarthCamera} as its rotation
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
    private GL3DQuatd currentDragRotation;

    protected GL3DTrackballRotationInteraction(GL3DSolarRotationTrackingTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.currentRotationEndPoint = getVectorFromSphere(e.getPoint(), camera);
        if (currentRotationStartPoint != null && currentRotationEndPoint != null) {
            currentDragRotation = GL3DQuatd.calcRotation(currentRotationStartPoint, currentRotationEndPoint);
        }
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

    protected GL3DVec3d getVectorFromSphere(Point viewportCoordinates, GL3DCamera camera) {
        GL3DState state = GL3DState.get();
        GL3DMat4d vpm = camera.getLocalRotation().toMatrix().inverse();
        GL3DMat4d tli = GL3DMat4d.identity();
        System.out.println("viewportCoordinates" + viewportCoordinates);
        System.out.println("viewport" + state.getViewportWidth() + " " + state.getViewportHeight());

        GL3DVec4d centeredViewportCoordinates = new GL3DVec4d(2. * (2. * viewportCoordinates.getX() / state.getViewportWidth() - 0.5) * state.getViewportWidth() / state.getViewportHeight(), 2. * (2. * viewportCoordinates.getY() / state.getViewportHeight() - 0.5), 0., 0.);
        System.out.println(centeredViewportCoordinates);
        GL3DVec4d solarCoordinates = vpm.multiply(centeredViewportCoordinates);
        solarCoordinates.w = 1.;
        solarCoordinates = tli.multiply(solarCoordinates);
        solarCoordinates.w = 0.;

        double solarCoordinates3Dz = Math.sqrt(1 - solarCoordinates.dot(solarCoordinates));
        if (solarCoordinates3Dz == Double.NaN) {
            solarCoordinates3Dz = 0.;
        }
        GL3DVec3d solarCoordinates3D = new GL3DVec3d(solarCoordinates.y, solarCoordinates.x, solarCoordinates3Dz);
        System.out.println(solarCoordinates3D);

        return solarCoordinates3D;
    }

}
