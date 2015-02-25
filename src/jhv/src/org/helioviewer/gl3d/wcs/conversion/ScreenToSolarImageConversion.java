package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.ScreenCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;

public class ScreenToSolarImageConversion implements CoordinateConversion {

    private SolarImageCoordinateSystem solarImageCoordinateSystem;

    private ScreenCoordinateSystem screenCoordinateSystem;

    public ScreenToSolarImageConversion(ScreenCoordinateSystem screenCoordinateSystem, SolarImageCoordinateSystem solarImageCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.screenCoordinateSystem = screenCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return solarImageCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.screenCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double px = vector.getValue(ScreenCoordinateSystem.X_COORDINATE);
        double py = vector.getValue(ScreenCoordinateSystem.Y_COORDINATE);

        double width = this.screenCoordinateSystem.getViewport().getWidth();
        double height = this.screenCoordinateSystem.getViewport().getHeight();
        // px *= this.solarDiskCoordinateSystem.getSolarRadius();
        // py *= this.solarDiskCoordinateSystem.getSolarRadius();
        //
        // double width = this.screenCoordinateSystem.getRegion().getWidth();
        // double height = this.screenCoordinateSystem.getRegion().getHeight();

        double x = (2 * px / width) - 1;
        double y = 1 - (2 * py / height);

        double x2 = Math.pow(x, 2.0);
        double y2 = Math.pow(y, 2.0);

        if (x2 + y2 > 1.0) {
            x /= (x2 + y2);
            y /= (x2 + y2);
        }

        x *= this.solarImageCoordinateSystem.getSolarRadius();
        y *= this.solarImageCoordinateSystem.getSolarRadius();

        return this.solarImageCoordinateSystem.createCoordinateVector(x, y);
    }
}
