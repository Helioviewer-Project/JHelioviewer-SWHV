package org.helioviewer.gl3d.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.base.math.GL3DQuatd;
import org.helioviewer.base.math.GL3DVec3d;
import org.helioviewer.jhv.display.Displayer;

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

    protected GL3DTrackballRotationInteraction(GL3DSolarRotationTrackingTrackballCamera camera) {
        super(camera);
    }

    @Override
    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.currentRotationEndPoint = camera.getVectorFromSphereAlt(e.getPoint());
        if (currentRotationStartPoint != null && currentRotationEndPoint != null) {
            currentDragRotation = GL3DQuatd.calcRotation(currentRotationStartPoint, currentRotationEndPoint);
        }
        camera.rotateCurrentDragRotation(currentDragRotation);
        this.camera.updateCameraTransformation();

        Displayer.display();
    }

    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        this.currentRotationStartPoint = null;
        this.currentRotationEndPoint = null;
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
        this.currentRotationStartPoint = camera.getVectorFromSphereAlt(e.getPoint());
    }

}
