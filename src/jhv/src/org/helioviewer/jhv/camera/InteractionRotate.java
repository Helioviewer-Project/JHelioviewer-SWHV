package org.helioviewer.jhv.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.display.Displayer;

public class InteractionRotate extends InteractionDefault {

    private Vec3 currentRotationStartPoint;

    protected InteractionRotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Vec3 currentRotationEndPoint = camera.getVectorFromSphereTrackball(e.getPoint());
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint));
        Displayer.render();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        currentRotationStartPoint = null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setActiveView(e);
        currentRotationStartPoint = camera.getVectorFromSphereTrackball(e.getPoint());
    }

}
