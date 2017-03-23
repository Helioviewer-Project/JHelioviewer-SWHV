package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;

import com.jogamp.newt.event.MouseEvent;

public class InteractionRotate extends Interaction {

    private Vec3 currentRotationStartPoint;

    public InteractionRotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Displayer.getActiveViewport(), e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRotationStartPoint == null) // freak crash
            return;

        Vec3 currentRotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Displayer.getActiveViewport(), e.getX(), e.getY());
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint));
        Displayer.render(0.5);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Displayer.render(1);
    }

}
