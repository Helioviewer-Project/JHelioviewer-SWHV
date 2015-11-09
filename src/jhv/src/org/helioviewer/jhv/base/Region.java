package org.helioviewer.jhv.base;

import org.helioviewer.jhv.base.math.Vec2;

public class Region {

    private final Vec2 lowerLeftCorner;
    private final Vec2 sizeVector;

    public Region(double newLowerLeftX, double newLowerLeftY, double newWidth, double newHeight) {
        lowerLeftCorner = new Vec2(newLowerLeftX, newLowerLeftY);
        sizeVector = new Vec2(newWidth, newHeight);
    }

    public Region(Vec2 newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vec2(newWidth, newHeight);
    }

    public Region(double newLowerLeftX, double newLowerLeftY, Vec2 newSizeVector) {
        lowerLeftCorner = new Vec2(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    public Region(Vec2 newLowerLeftCorner, Vec2 newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    public Vec2 getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    public Vec2 getSize() {
        return sizeVector;
    }

    public double getHeight() {
        return sizeVector.y;
    }

    public double getWidth() {
        return sizeVector.x;
    }

    public Vec2 getLowerRightCorner() {
        return new Vec2(lowerLeftCorner.x + sizeVector.x, lowerLeftCorner.y);
    }

    public Vec2 getUpperLeftCorner() {
        return new Vec2(lowerLeftCorner.x, lowerLeftCorner.y + sizeVector.y);
    }

    public Vec2 getUpperRightCorner() {
        return Vec2.add(lowerLeftCorner, sizeVector);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Region)) {
            return false;
        }
        Region r = (Region) o;
        return r.getSize().equals(sizeVector) && r.getLowerLeftCorner().equals(lowerLeftCorner);
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

    @Override
    public String toString() {
        return "[Region: Corner: " + lowerLeftCorner + ", Size: " + sizeVector + "]";
    }

}
