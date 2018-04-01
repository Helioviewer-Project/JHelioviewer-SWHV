package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
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
        currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRotationStartPoint == null) // freak crash
            return;

        Vec3 currentRotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY());
        Vec3 axis = Display.getUpdateViewpoint() == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint).twist(axis));
        Display.display();
    }

}
