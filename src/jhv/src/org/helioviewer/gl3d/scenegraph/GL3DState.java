package org.helioviewer.gl3d.scenegraph;

import java.util.Date;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.viewmodel.view.opengl.GL3DComponentView;

/**
 * The {@link GL3DState} is recreated every render pass by the
 * {@link GL3DComponentView}. It provides the reference to the {@link GL2}
 * object and stores some globally relevant information such as width and height
 * of the viewport, etc. Also it allows for the stacking of the view
 * transformations.
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DState {

    private static GL3DState instance;

    public GL2 gl;

    protected GL3DCamera activeCamera;

    protected int viewportWidth;
    protected int viewportHeight;

    private Date currentObservationDate;

    public static GL3DState create(GL2 gl) {
        instance = new GL3DState(gl);
        return instance;
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

    private GL3DState(GL2 gl) {
        this.gl = gl;
    }

    public void setActiveChamera(GL3DCamera camera) {
        this.activeCamera = camera;
    }

    public GL3DCamera getActiveCamera() {
        return activeCamera;
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

}
