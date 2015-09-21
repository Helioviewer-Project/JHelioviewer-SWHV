package org.helioviewer.jhv.renderable.viewport;

import org.helioviewer.jhv.camera.GL3DCamera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.opengl.GLInfo;

public class GL3DViewport {
    private int w;
    private int h;
    private int x;
    private int y;
    private boolean isVisible = true;
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
        return GLInfo.pixelScale[0] * x;
    }

    public int getOffsetY() {
        return Displayer.getActiveViewport().getHeight() * GLInfo.pixelScale[1] - GLInfo.pixelScale[0] * y - GLInfo.pixelScale[1] * getHeight();
    }

    public void setViewportSize(int width, int height) {
        w = width;
        h = height;
    }

    public void setViewportOffset(int offsetX, int offsetY) {
        x = offsetX;
        y = offsetY;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }
}
