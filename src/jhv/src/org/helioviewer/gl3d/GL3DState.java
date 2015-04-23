package org.helioviewer.gl3d;

import java.util.Date;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DObserverCamera;

/**
 * The {@link GL3DState} is recreated every render pass by the
 * {@link ComponentView}. It provides the reference to the {@link GL2} object
 * and stores some globally relevant information such as width and height of the
 * viewport, etc. Also it allows for the stacking of the view transformations.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DState {

    private static GL3DState instance = new GL3DState(null);

    public GL2 gl;

    private static GL3DCamera activeCamera;

    private int viewportWidth;
    private int viewportHeight;

    private Date currentObservationDate;

    private GL3DState(GL2 gl) {
        this.gl = gl;
        activeCamera = new GL3DObserverCamera();
    }

    public static GL3DState get() {
        return instance;
    }

    public static GL3DState setUpdated(GL2 gl, int width, int height) {
        instance.gl = gl;
        instance.viewportWidth = width;
        instance.viewportHeight = height;
        return instance;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public Date getCurrentObservationDate() {
        return currentObservationDate;
    }

    public void setCurrentObservationDate(Date currentObservationDate) {
        this.currentObservationDate = currentObservationDate;
    }

    public static void setActiveCamera(GL3DCamera camera) {
        activeCamera.deactivate();
        camera.activate(activeCamera);
        activeCamera = camera;
    }

    public static GL3DCamera getActiveCamera() {
        return activeCamera;
    }

}
