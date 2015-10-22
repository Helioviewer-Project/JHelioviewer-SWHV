package org.helioviewer.jhv.base;

import org.helioviewer.jhv.base.math.Vec2d;

public class Region {

    private final Vec2d lowerLeftCorner;
    private final Vec2d sizeVector;

    public Region(double newLowerLeftX, double newLowerLeftY, double newWidth, double newHeight) {
        lowerLeftCorner = new Vec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = new Vec2d(newWidth, newHeight);
    }

    public Region(Vec2d newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vec2d(newWidth, newHeight);
    }

    public Region(double newLowerLeftX, double newLowerLeftY, Vec2d newSizeVector) {
        lowerLeftCorner = new Vec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    public Region(Vec2d newLowerLeftCorner, Vec2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    public Vec2d getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    public Vec2d getSize() {
        return sizeVector;
    }

    public double getHeight() {
        return sizeVector.y;
    }

    public double getWidth() {
        return sizeVector.x;
    }

    public Vec2d getLowerRightCorner() {
        return new Vec2d(lowerLeftCorner.x + sizeVector.x, lowerLeftCorner.y);
    }

    public Vec2d getUpperLeftCorner() {
        return new Vec2d(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    public Vec2d getUpperRightCorner() {
        return Vec2d.add(lowerLeftCorner, sizeVector);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Region)) {
            return false;
        }
        Region r = (Region) o;
        return r.getSize().equals(sizeVector) && r.getLowerLeftCorner().equals(lowerLeftCorner);
    }

    public String toString() {
        return "[Region: Corner: " + lowerLeftCorner + ", Size: " + sizeVector + "]";
    }

}
