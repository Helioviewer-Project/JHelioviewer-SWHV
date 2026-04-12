package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Display;

public class GLGrab {

    public final int w;
    public final int h;
    private GLFrameCapture capture;

    public GLGrab(int _w, int _h) {
        w = _w;
        h = _h;
    }

    private void init() {
        capture = new GLFrameCapture(w, h, GL.SAMPLES);
    }

    public void dispose() {
        if (capture != null) {
            capture.dispose();
            capture = null;
        }
    }

    public void renderFrame(Camera camera, ByteBuffer buffer) {
        if (capture == null)
            init();

        int _x = Display.fullViewport.x;
        int _y = Display.fullViewport.yGL;
        int _w = Display.fullViewport.width;
        int _h = Display.fullViewport.height;

        try {
            Display.setGLSize(0, 0, w, h);
            Display.reshapeAll();

            capture.bindForRender();
            if (Display.mode.isOrthographic()) {
                GLRenderer.renderScene(camera);
            } else {
                GLRenderer.renderSceneScale(camera);
            }
            capture.readPixels(buffer);
        } finally {
            Display.setGLSize(_x, _y, _w, _h);
            Display.reshapeAll();
        }
    }

}
