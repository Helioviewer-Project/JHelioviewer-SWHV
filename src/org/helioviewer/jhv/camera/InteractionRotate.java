package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

import com.jogamp.newt.event.MouseEvent;

public class InteractionRotate extends Interaction {

    private Vec3 currentRotationStartPoint;

    public InteractionRotate(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), Sun.Radius2);
        double len2 = currentRotationStartPoint.length2();
        if (len2 > Sun.Radius2) {
            double r = 0.5 * camera.getCameraWidth();
            currentRotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), r * r);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRotationStartPoint == null) // freak crash
            return;

        double len2 = currentRotationStartPoint.length2();
        Vec3 currentRotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), len2);

        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint));
        Display.display();
    }

}
