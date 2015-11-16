package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;

public class InteractionRotate extends Interaction {

    private Vec3 currentRotationStartPoint;

    public InteractionRotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vec3 currentRotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Displayer.getViewport(), e.getPoint());
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint));
        Displayer.render();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Displayer.getViewport(), e.getPoint());
    }

}
