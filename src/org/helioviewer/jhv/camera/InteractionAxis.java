package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

class InteractionAxis implements InteractionType {

    private final Camera camera;
    private Vec3 currentRotationStartPoint;

    InteractionAxis(Camera _camera) {
        camera = _camera;
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

        Vec3 axis = camera.getUpdateViewpoint() == UpdateViewpoint.equatorial ? Vec3.ZAxis : Vec3.YAxis;
        camera.rotateCurrentDragRotation(Quat.calcRotation(currentRotationStartPoint, currentRotationEndPoint).twist(axis));
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

}
