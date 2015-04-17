package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;

/**
 * Implementation of {@link Region}.
 * 
 * @author Ludwig Schmidt
 * */
public class RegionAdapter implements Region {

    private final BasicRegion region;

    /**
     * Default constructor.
     * 
     * @param newRegion
     *            BasicRegion object which holds the minimal region description.
     * */
    public RegionAdapter(BasicRegion newRegion) {
        region = newRegion;
    }

    /**
     * {@inheritDoc}
     * */
    public double getCornerX() {
        return region.getLowerLeftCorner().getX();
    }

    /**
     * {@inheritDoc}
     * */
    public double getCornerY() {
        return region.getLowerLeftCorner().getY();
    }

    /**
     * {@inheritDoc}
     * */
    public double getHeight() {
        return region.getSize().getY();
    }

    /**
     * {@inheritDoc}
     * */
    public double getWidth() {
        return region.getSize().getX();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getLowerLeftCorner() {
        return region.getLowerLeftCorner();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getSize() {
        return region.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    public RectangleDouble getRectangle() {
        return new RectangleDouble(region.getLowerLeftCorner(), region.getSize());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getLowerRightCorner() {
        return region.getLowerLeftCorner().add(region.getSize().getXVector());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getUpperLeftCorner() {
        return region.getLowerLeftCorner().add(region.getSize().getYVector());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dDouble getUpperRightCorner() {
        return region.getLowerLeftCorner().add(region.getSize());
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (!(o instanceof Region)) {
            return false;
        }

        Region r = (Region) o;
        return r.getSize().equals(getSize()) && r.getLowerLeftCorner().equals(getLowerLeftCorner());

    }

    /**
     * {@inheritDoc}
     */

    public String toString() {
        return "[RegionAdapter: Rectangle " + this.getRectangle() + "]";
    }
}
