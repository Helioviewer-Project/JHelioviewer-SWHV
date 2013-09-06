package org.helioviewer.gl3d.wcs.impl;

import org.helioviewer.gl3d.wcs.Cartesian2DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.Unit;
import org.helioviewer.gl3d.wcs.conversion.ScreenToSolarImageConversion;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * The ScreenCoordinateSystem can be used to convert Screen Points to other
 * {@link CoordinateSystem}s.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class ScreenCoordinateSystem extends Cartesian2DCoordinateSystem {

    private Viewport viewport;
    private Region region;

    public ScreenCoordinateSystem(Viewport viewport, Region region) {
        super(Unit.Pixel);
        this.viewport = viewport;
        this.region = region;
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem.getClass().isAssignableFrom(SolarImageCoordinateSystem.class)) {
            return new ScreenToSolarImageConversion(this, (SolarImageCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Region getRegion() {
        return this.region;
    }
}
