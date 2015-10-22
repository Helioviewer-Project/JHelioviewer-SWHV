package org.helioviewer.jhv.base;

import org.helioviewer.jhv.base.math.Vector2dInt;

public class Viewport {

    private final Vector2dInt sizeVector;

    public Viewport(Vector2dInt _sizeVector) {
        sizeVector = _sizeVector;
    }

    public Viewport(int width, int height) {
        sizeVector = new Vector2dInt(width, height);
    }

    public int getHeight() {
        return sizeVector.getY();
    }

    public int getWidth() {
        return sizeVector.getX();
    }

    public Vector2dInt getSize() {
        return sizeVector;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Viewport)) {
            return false;
        }

        Viewport v = (Viewport) o;
        return v.getHeight() == getHeight() && v.getWidth() == getWidth();
    }

    @Override
    public String toString() {
        return "[Viewport: Size: " + sizeVector + "]";
    }

}
