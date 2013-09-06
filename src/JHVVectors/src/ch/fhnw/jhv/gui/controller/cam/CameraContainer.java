/**
 * 
 */
package ch.fhnw.jhv.gui.controller.cam;

import java.util.ArrayList;

/**
 * Contains several Camera implementations
 * 
 * @author Robin Oster (robin.oster@students.fhnw.ch)
 * 
 */
public class CameraContainer {

    /**
     * Camera Type
     * 
     * @author Robin Oster (robin.oster@students.fhnw.ch)
     * 
     */
    public enum CameraType {
        ROTATION_PLANE, ROTATION_SUN, TRACKBALL, FREEFLIGHT;
    }

    /**
     * Get a camera, always return a new Implementation
     * 
     * @param CameraType
     *            type
     * 
     * @return Camera camera
     */
    public static Camera getCamera(CameraType type) {
        switch (type) {
        case ROTATION_PLANE:
            return new PlaneRotationCamera();

        case ROTATION_SUN:
            return new LookAtCamera();

        case TRACKBALL:
            return new TrackBallCamera();

        case FREEFLIGHT:
            return new FreeFlightCamera();
        }

        // TODO: Or return a empty camera which does nothing? (NullObject
        // Pattern)
        return null;
    }

    /**
     * Return a list of cams which could be used in the plane visualization mode
     * 
     * @return ArrayList<Camera> List of all available cams for this mode
     */
    public static ArrayList<Camera> getCamsPlane() {
        ArrayList<Camera> cams = new ArrayList<Camera>();

        cams.add(getCamera(CameraType.ROTATION_PLANE));
        cams.add(getCamera(CameraType.TRACKBALL));

        return cams;
    }

    /**
     * Return a list of cams which could be used in the sun visualization mode
     * 
     * @return ArrayList<Camera> List of all available cams for this mode
     */
    public static ArrayList<Camera> getCamsSun() {
        ArrayList<Camera> cams = new ArrayList<Camera>();

        cams.add(getCamera(CameraType.ROTATION_SUN));
        cams.add(getCamera(CameraType.TRACKBALL));

        return cams;
    }
}
