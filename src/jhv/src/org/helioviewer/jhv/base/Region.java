package org.helioviewer.jhv.base;

import org.helioviewer.jhv.base.math.GL3DVec2d;

public class Region {

    private final GL3DVec2d lowerLeftCorner;
    private final GL3DVec2d sizeVector;

    public Region(double newLowerLeftX, double newLowerLeftY, double newWidth, double newHeight) {
        lowerLeftCorner = new GL3DVec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = new GL3DVec2d(newWidth, newHeight);
    }

    public Region(GL3DVec2d newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new GL3DVec2d(newWidth, newHeight);
    }

    public Region(double newLowerLeftX, double newLowerLeftY, GL3DVec2d newSizeVector) {
        lowerLeftCorner = new GL3DVec2d(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    public Region(GL3DVec2d newLowerLeftCorner, GL3DVec2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    public GL3DVec2d getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    public GL3DVec2d getSize() {
        return sizeVector;
    }

    public double getHeight() {
        return sizeVector.y;
    }

    public double getWidth() {
        return sizeVector.x;
    }

    public GL3DVec2d getLowerRightCorner() {
        return new GL3DVec2d(lowerLeftCorner.x + sizeVector.x, lowerLeftCorner.y);
    }

    public GL3DVec2d getUpperLeftCorner() {
        return new GL3DVec2d(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    public GL3DVec2d getUpperRightCorner() {
        return GL3DVec2d.add(lowerLeftCorner, sizeVector);
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
