package org.helioviewer.jhv.camera;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;
import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec3;

class InteractionRotate implements InteractionType {

    private final Camera camera;
    private Vec3 rotationStartPoint;

    InteractionRotate(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        rotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), Sun.Radius2);
        double len2 = rotationStartPoint.length2();
        if (len2 > Sun.Radius2) {
            double r = 0.5 * camera.getCameraWidth();
            rotationStartPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), r * r);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (rotationStartPoint == null) // freak crash
            return;

        double len2 = rotationStartPoint.length2();
        Vec3 rotationEndPoint = CameraHelper.getVectorFromSphereTrackball(camera, Display.getActiveViewport(), e.getX(), e.getY(), len2);

        camera.rotateDragRotation(Quat.calcRotation(rotationStartPoint, rotationEndPoint));
        MovieDisplay.display();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

}
