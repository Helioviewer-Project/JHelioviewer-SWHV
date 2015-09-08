package org.helioviewer.jhv.renderable.viewport;

import org.helioviewer.jhv.camera.GL3DCamera;

public class GL3DViewport {
    private final int w;
    private final int h;
    private final int x;
    private final int y;
    private GL3DCamera camera;

    public GL3DViewport(int _x, int _y, int _w, int _h, GL3DCamera _camera) {
        w = _w;
        h = _h;
        x = _x;
        y = _y;
        camera = _camera;
    }

    public GL3DCamera getCamera() {
        return camera;
    }

    public void setCamera(GL3DCamera _camera) {
        camera = _camera;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int getOffsetX() {
        return x;
    }

    public int getOffsetY() {
        return y;
    }

}
