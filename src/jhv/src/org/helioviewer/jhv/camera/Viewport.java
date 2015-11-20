package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;

public class Viewport {

    public final int width;
    public final int height;
    private final int x;
    private final int y;
    public final int index;

    private boolean active;

    public Viewport(int _idx, int _x, int _y, int _w, int _h, boolean _active) {
        index = _idx;
        width = _w;
        height = _h;
        x = _x;
        y = _y;
        active = _active;
    }

    public Viewport(Viewport vp, int _x, int _y, int _w, int _h) {
        this(vp.index, _x, _y, _w, _h, vp.active);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return Displayer.getGLHeight() - height - y;
    }

    public boolean contains(int px, int py) {
        if (px >= x && px < x + width && py >= getY() && py < getY() + height) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Offset: " + getX() + "," + getY() + " Size: " + width + "," + height;
    }

    public boolean isActive() {
        return active;
    }

    public void computeActive() {
        active = ImageViewerGui.getRenderableContainer().isViewportActive(index);
    }

}
