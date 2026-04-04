package org.helioviewer.jhv.opengl;

import java.nio.Buffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;

import com.jogamp.opengl.GL3;

public class GLGrab {

    public final int w;
    public final int h;
    private GLFrameCapture capture;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    private void init(GL3 gl) {
        capture = new GLFrameCapture(gl, w, h, GLInfo.GLSAMPLES);
    }

    public void dispose(GL3 gl) {
        if (capture != null) {
            capture.dispose(gl);
            capture = null;
        }
    }

    public void renderFrame(Camera camera, GL3 gl, Buffer buffer) {
        if (capture == null)
            init(gl);

        int _x = Display.fullViewport.x;
        int _y = Display.fullViewport.yGL;
        int _w = Display.fullViewport.width;
        int _h = Display.fullViewport.height;

        try {
            Display.setGLSize(0, 0, w, h);
            Display.reshapeAll();

            capture.bindForRender(gl);
            if (Display.mode.isOrthographic()) {
                GLListener.renderScene(camera, gl);
            } else {
                GLListener.renderSceneScale(camera, gl);
            }
            capture.readPixels(gl, buffer);
        } finally {
            Display.setGLSize(_x, _y, _w, _h);
            Display.reshapeAll();
        }
    }

}
