package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

import com.jogamp.newt.event.MouseEvent;

public class InteractionAxis extends Interaction {

    private Vec3 currentRotationStartPoint;

    public InteractionAxis(Camera _camera) {
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
        Vec3 axis = Displayer.getUpdateViewpoint() == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint).twist(axis));
        Displayer.render(0.5);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Displayer.render(1);
    }

}
