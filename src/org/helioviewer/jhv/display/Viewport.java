package org.helioviewer.jhv.display;

import java.nio.FloatBuffer;

public class Viewport {

    public final double aspect;
    public final int idx;

    public final int width;
    public final int height;
    public final int x;
    public final int yGL; // GL direction
    public final int yAWT; // AWT direction
    public final FloatBuffer toGLSL;

    public Viewport(int _idx, int _x, int _y, int _w, int _h) {
        idx = _idx;
        width = _w;
        height = _h;
        aspect = _w / (double) _h;
        x = _x;
        yGL = Display.glHeight - height - _y;
        yAWT = _y;

        toGLSL = FloatBuffer.wrap(new float[]{width, height, height / width, 0, x, yGL});
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= yAWT && py < yAWT + height;
    }

    @Override
    public String toString() {
        return "Offset: " + x + ',' + yGL + " Size: " + width + ',' + height;
    }

}
