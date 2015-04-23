package org.helioviewer.gl3d;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DObserverCamera;

/**
 * The {@link GL3DState} is updated every render pass by the
 * {@link ComponentView}. It provides the reference to the {@link GL2} object
 * and stores some globally relevant information such as width and height of the
 * viewport, etc.
 *
 */
public class GL3DState {

    private final static GL3DState instance = new GL3DState(null);

    public static GL2 gl;

    private static GL3DCamera activeCamera;
    private static int viewportWidth;
    private static int viewportHeight;

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

    public static int getViewportHeight() {
        return viewportHeight;
    }

    public static int getViewportWidth() {
        return viewportWidth;
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
