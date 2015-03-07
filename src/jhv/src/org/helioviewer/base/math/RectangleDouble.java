package org.helioviewer.base.math;

public final class RectangleDouble {

    private final Vector2dDouble corner;
    private final Vector2dDouble size;

    public RectangleDouble(final double newX, final double newY, final double newWidth, final double newHeight) {
        corner = new Vector2dDouble(newX, newY);
        size = new Vector2dDouble(newWidth, newHeight);
    }

    public RectangleDouble(final double newX, final double newY, final Vector2dDouble newSize) {
        corner = new Vector2dDouble(newX, newY);
        size = newSize;
    }

    public RectangleDouble(final Vector2dDouble newCorner, final double newWidth, final double newHeight) {
        corner = newCorner;
        size = new Vector2dDouble(newHeight, newWidth);
    }

    public RectangleDouble(final Vector2dDouble newCorner, final Vector2dDouble newSize) {
        corner = newCorner;
        size = newSize;
    }

    public double getX() {
        return corner.getX();
    }

    public double getY() {
        return corner.getY();
    }

    public double getWidth() {
        return size.getX();
    }

    public double getHeight() {
        return size.getY();
    }

    public Vector2dDouble getLowerLeftCorner() {
        return corner;
    }

    public Vector2dDouble getLowerRightCorner() {
        return corner.add(size.getXVector());
    }

    public Vector2dDouble getUpperLeftCorner() {
        return corner.add(size.getYVector());
    }

    public Vector2dDouble getUpperRightCorner() {
        return corner.add(size);
    }

    public Vector2dDouble getSize() {
        return size;
    }

    public double area() {
        return size.getX() * size.getY();
    }

    public double aspectRatio() {
        return size.getX() / size.getY();
    }

    public boolean isInsideOuterRectangle(final RectangleDouble outer) {
        return getX() >= outer.getX() && getY() >= outer.getY() && getX() + getWidth() <= outer.getX() + outer.getWidth() && getY() + getHeight() <= outer.getY() + outer.getHeight();
    }

    public static boolean isInsideOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.isInsideOuterRectangle(outer);
    }

    public RectangleDouble cropToOuterRectangle(final RectangleDouble outer) {
        Vector2dDouble newCorner = corner.crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        Vector2dDouble newUpperRight = getUpperRightCorner().crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        return new RectangleDouble(newCorner, newUpperRight.subtract(newCorner));
    }

    public static RectangleDouble cropToOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.cropToOuterRectangle(outer);
    }

    public RectangleDouble moveAndCropToOuterRectangle(final RectangleDouble outer) {
        Vector2dDouble newSize = size.crop(Vector2dDouble.NULL_VECTOR, outer.size);
        Vector2dDouble croppedCorner = corner.crop(outer.getLowerLeftCorner(), outer.getUpperRightCorner());
        Vector2dDouble newCorner = croppedCorner.subtract(croppedCorner.add(newSize).subtract(outer.getUpperRightCorner()).crop(Vector2dDouble.NULL_VECTOR, Vector2dDouble.POSITIVE_INFINITY_VECTOR));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble moveAndCropToOuterRectangle(final RectangleDouble inner, final RectangleDouble outer) {
        return inner.moveAndCropToOuterRectangle(outer);
    }

    public RectangleDouble expandToAspectRatioKeepingCenter(final double newAspectRatio) {
        Vector2dDouble newSize;
        if (size.getX() / size.getY() < newAspectRatio) {
            newSize = new Vector2dDouble(newAspectRatio * size.getY(), size.getY());
        } else {
            newSize = new Vector2dDouble(size.getX(), size.getX() / newAspectRatio);
        }
        Vector2dDouble newCorner = corner.add((size.subtract(newSize).scale(0.5)));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble expandToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.expandToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble contractToAspectRatioKeepingCenter(final double newAspectRatio) {
        Vector2dDouble newSize;
        if (size.getX() / size.getY() < newAspectRatio) {
            newSize = new Vector2dDouble(size.getX(), size.getX() / newAspectRatio);
        } else {
            newSize = new Vector2dDouble(newAspectRatio * size.getY(), size.getY());
        }
        Vector2dDouble newCorner = corner.add((size.subtract(newSize).scale(0.5)));
        return new RectangleDouble(newCorner, newSize);
    }

    public static RectangleDouble contractToAspectRatioKeepingCenter(final RectangleDouble rectangle, final double newAspectRatio) {
        return rectangle.contractToAspectRatioKeepingCenter(newAspectRatio);
    }

    public RectangleDouble getBoundingRectangle(final RectangleDouble r) {
        Vector2dDouble newLowerLeftCorner = corner.componentMin(r.getLowerLeftCorner());
        Vector2dDouble newUpperRightCorner = getUpperRightCorner().componentMax(r.getUpperRightCorner());
        return new RectangleDouble(newLowerLeftCorner, newUpperRightCorner.subtract(newLowerLeftCorner));
    }

    public static RectangleDouble getBoundingRectangle(final RectangleDouble r1, final RectangleDouble r2) {
        return r1.getBoundingRectangle(r2);
    }

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

    public String toString() {
        return "[Rectangle: Corner: " + corner + ", Size: " + size + "]";
    }

}
