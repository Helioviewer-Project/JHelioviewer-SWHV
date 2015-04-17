package org.helioviewer.base.math;

import org.helioviewer.gl3d.math.GL3DVec2d;

public final class RectangleDouble {

    private final GL3DVec2d corner;
    private final GL3DVec2d size;

    public RectangleDouble(final double newX, final double newY, final double newWidth, final double newHeight) {
        corner = new GL3DVec2d(newX, newY);
        size = new GL3DVec2d(newWidth, newHeight);
    }

    public RectangleDouble(final double newX, final double newY, final GL3DVec2d newSize) {
        corner = new GL3DVec2d(newX, newY);
        size = newSize;
    }

    public RectangleDouble(final GL3DVec2d newCorner, final double newWidth, final double newHeight) {
        corner = newCorner;
        size = new GL3DVec2d(newHeight, newWidth);
    }

    public RectangleDouble(final GL3DVec2d newCorner, final GL3DVec2d newSize) {
        corner = newCorner;
        size = newSize;
    }

    public double getX() {
        return corner.x;
    }

    public double getY() {
        return corner.y;
    }

    public double getWidth() {
        return size.x;
    }

    public double getHeight() {
        return size.y;
    }

    public GL3DVec2d getLowerLeftCorner() {
        return corner;
    }

    public GL3DVec2d getLowerRightCorner() {
        return GL3DVec2d.add(corner, size.getXVector());
    }

    public GL3DVec2d getUpperLeftCorner() {
        return GL3DVec2d.add(corner, size.getYVector());
    }

    public GL3DVec2d getUpperRightCorner() {
        return GL3DVec2d.add(corner, size);
    }

    public GL3DVec2d getSize() {
        return size;
    }

    public double area() {
        return size.x * size.y;
    }

    public double aspectRatio() {
        return size.x / size.y;
    }

    public boolean isInsideOuterRectangle(final RectangleDouble outer) {
        return getX() >= outer.getX() && getY() >= outer.getY() && getX() + getWidth() <= outer.getX() + outer.getWidth() && getY() + getHeight() <= outer.getY() + outer.getHeight();
    }

    public static boolean isInsideOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.isInsideOuterRectangle(outer);
    }

    public RectangleDouble expandToAspectRatioKeepingCenter(final double newAspectRatio) {
        GL3DVec2d newSize;
        if (size.x / size.y < newAspectRatio) {
            newSize = new GL3DVec2d(newAspectRatio * size.y, size.y);
        } else {
            newSize = new GL3DVec2d(size.x, size.x / newAspectRatio);
        }
        GL3DVec2d newCorner = GL3DVec2d.add(corner, GL3DVec2d.scale(GL3DVec2d.subtract(size, newSize), 0.5));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble expandToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.expandToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble contractToAspectRatioKeepingCenter(final double newAspectRatio) {
        GL3DVec2d newSize;
        if (size.x / size.y < newAspectRatio) {
            newSize = new GL3DVec2d(size.x, size.y / newAspectRatio);
        } else {
            newSize = new GL3DVec2d(newAspectRatio * size.y, size.y);
        }
        GL3DVec2d newCorner = GL3DVec2d.add(corner, (GL3DVec2d.scale(GL3DVec2d.subtract(size, newSize), 0.5)));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble contractToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.contractToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble getBoundingRectangle(final RectangleDouble r) {
        GL3DVec2d newLowerLeftCorner = GL3DVec2d.componentMin(corner, r.getLowerLeftCorner());
        GL3DVec2d newUpperRightCorner = GL3DVec2d.componentMax(getUpperRightCorner(), r.getUpperRightCorner());
        return new RectangleDouble(newLowerLeftCorner, GL3DVec2d.subtract(newUpperRightCorner, newLowerLeftCorner));
    }

    public static RectangleDouble getBoundingRectangle(final RectangleDouble r1, final RectangleDouble r2) {
        return r1.getBoundingRectangle(r2);
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof RectangleDouble)) {
            return false;
        }
        RectangleDouble r = (RectangleDouble) o;

        return corner.equals(r.corner) && size.equals(r.size);
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public String toString() {
        return "[Rectangle: Corner: " + corner + ", Size: " + size + "]";
    }

}
