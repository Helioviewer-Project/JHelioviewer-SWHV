package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quatd;
import org.helioviewer.jhv.base.math.Vec3d;
import org.helioviewer.jhv.display.Displayer;

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
            camera.rotateCurrentDragRotation(currentDragRotation);
            camera.updateCameraTransformation();
            Displayer.render();
        }
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
