package org.helioviewer.base;

import org.helioviewer.base.math.Vector2dInt;

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

    public boolean equals(Viewport v) {
        if (!(v instanceof Viewport)) {
            return false;
        }
        return v.getHeight() == getHeight() && v.getWidth() == getWidth();
    }

    public String toString() {
        return "[Viewport: Size: " + sizeVector + "]";
    }

}
