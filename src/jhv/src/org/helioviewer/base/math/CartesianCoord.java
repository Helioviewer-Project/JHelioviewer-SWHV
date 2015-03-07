package org.helioviewer.base.math;

public class CartesianCoord {

    public double x = 0.0;
    public double y = 0.0;
    public double z = 0.0;

    public CartesianCoord() {

    }

    public CartesianCoord(Vector3dDouble vector) {
        x = vector.getX();
        y = vector.getY();
        z = vector.getZ();
    }

    public String toString() {
        return x + " " + y + " " + z;
    }

}
