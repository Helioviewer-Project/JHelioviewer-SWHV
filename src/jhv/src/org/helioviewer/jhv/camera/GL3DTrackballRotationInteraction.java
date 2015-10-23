package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec3d;
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

    private Vec3d currentRotationStartPoint;
    private Quatd currentDragRotation;

    protected GL3DTrackballRotationInteraction(GL3DCamera camera) {
        super(camera);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRotationStartPoint != null) {
            Vec3d currentRotationEndPoint = camera.getVectorFromSphereTrackball(e.getPoint());
            currentDragRotation = Quatd.calcRotation(currentRotationStartPoint, currentRotationEndPoint);
        }
        camera.rotateCurrentDragRotation(currentDragRotation);
        camera.updateCameraTransformation();
        Displayer.render();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        currentRotationStartPoint = null;
    }

    @Override
    public void reset() {
        if (currentDragRotation != null) {
            currentDragRotation.clear();
        }
        super.reset();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setActiveView(e);
        currentRotationStartPoint = camera.getVectorFromSphereTrackball(e.getPoint());
    }

}
