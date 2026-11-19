package org.helioviewer.jhv.opengl;

import java.nio.ByteBuffer;

import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.MapView;
import org.helioviewer.jhv.display.Viewport;

public class GLGrab {

    public final int w;
    public final int h;
    private final float pixelsPerPoint;
    private GLFrameCapture capture;

    public GLGrab(int _w, int _h) {
        this(_w, _h, fixedSizePixelsPerPoint(_w, _h));
    }

    public GLGrab(int _w, int _h, float _pixelsPerPoint) {
        w = _w;
        h = _h;
        pixelsPerPoint = _pixelsPerPoint;
    }

    public static float fixedSizePixelsPerPoint(int width, int height) {
        // The 1024×1024 preset uses one output pixel per point.
        return Math.min(width, height) / 1024f;
    }

    private void init() {
        capture = new GLFrameCapture(w, h);
    }

    public void dispose() {
        if (capture != null) {
            capture.dispose();
            capture = null;
        }
    }

    public void renderFrame(ByteBuffer buffer) {
        if (capture == null)
            init();

        Viewport screen = Display.fullViewport;
        float screenPixelsPerPoint = Display.getRenderPixelsPerPoint();

        try {
            Display.setGLSize(0, 0, w, h, pixelsPerPoint);
            Display.reshapeAll();
            MapView exportView = GLRenderer.createMapView(Display.getCamera(), GLRenderer.getDisplayedViewpoint());

            capture.bindForRender();
            GL.glClear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT);
            GLRenderer.renderScene(exportView);
            capture.readPixels(buffer);
        } finally {
            GL.glBindFramebuffer(GL.FRAMEBUFFER, 0);
            Display.setGLSize(screen.x, screen.yAWT, screen.width, screen.height, screenPixelsPerPoint);
            Display.reshapeAll();
        }
    }

}
