package org.helioviewer.viewmodel.region;

import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.RectangleDouble;

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
    @Override
    public double getCornerX() {
        return region.getLowerLeftCorner().x;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public double getCornerY() {
        return region.getLowerLeftCorner().y;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public double getHeight() {
        return region.getSize().y;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public double getWidth() {
        return region.getSize().x;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getLowerLeftCorner() {
        return region.getLowerLeftCorner();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getSize() {
        return region.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public RectangleDouble getRectangle() {
        return new RectangleDouble(region.getLowerLeftCorner(), region.getSize());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getLowerRightCorner() {
        return GL3DVec2d.add(region.getLowerLeftCorner(), region.getSize().getXVector());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getUpperLeftCorner() {
        return GL3DVec2d.add(region.getLowerLeftCorner(), region.getSize().getYVector());
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public GL3DVec2d getUpperRightCorner() {
        return GL3DVec2d.add(region.getLowerLeftCorner(), region.getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    @Override
    public String toString() {
        return "[RegionAdapter: Rectangle " + this.getRectangle() + "]";
    }
}
