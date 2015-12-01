package org.helioviewer.jhv.display;

public class Viewport {

    public final double aspect;
    public final int width;
    public final int height;
    public final int x;
    public final int y;
    public final int idx;

    private final int yAwt;

    public Viewport(int _idx, int _x, int _y, int _w, int _h) {
        idx = _idx;
        width = _w;
        height = _h;
        aspect = _w / (double) _h;
        x = _x;
        y = Displayer.getGLHeight() - height - _y;
        yAwt = _y;
    }

    public boolean contains(int px, int py) {
        if (px >= x && px < x + width && py >= yAwt && py < yAwt + height) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Offset: " + x + "," + y + " Size: " + width + "," + height;
    }

}
