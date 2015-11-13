package org.helioviewer.jhv.camera;

import java.awt.Dimension;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;

public class Viewport {

    private int w;
    private int h;
    private int x;
    private int y;
    private final int idx;
    private boolean isVisible = true;

    public Viewport(int _idx, int _x, int _y, int _w, int _h) {
        this(_idx, _x, _y, _w, _h, false);
    }

    private boolean active;

    public Viewport(int _idx, int _x, int _y, int _w, int _h, boolean _active) {
        idx = _idx;
        w = _w;
        h = _h;
        x = _x;
        y = _y;
        active = _active;
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
        return Displayer.getGLHeight() - h - y;
    }

    public Dimension getSize() {
        return new Dimension(w, h);
    }

    public void setSize(int _x, int _y, int _w, int _h) {
        w = _w;
        h = _h;
        x = _x;
        y = _y;
    }

    public void setSize(int _w, int _h) {
        w = _w;
        h = _h;
    }

    public void setOffset(int offsetX, int offsetY) {
        x = offsetX;
        y = offsetY;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }

    @Override
    public String toString() {
        return "Offset: " + getOffsetX() + "," + getOffsetY() + " Size: " + getWidth() + "," + getHeight();
    }

    public int getIndex() {
        return idx;
    }

    public boolean isActive() {
        return active;
    }

    public void computeActive() {
        active = ImageViewerGui.getRenderableContainer().isViewportActive(idx);
    }

}
