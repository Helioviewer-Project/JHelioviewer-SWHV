package org.helioviewer.gl3d.wcs.conversion;

import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.TextureCoordinateSystem;

public class SolarImageToTextureConversion implements CoordinateConversion {
    private SolarImageCoordinateSystem solarImageCoordinateSystem;
    private TextureCoordinateSystem textureCoordinateSystem;

    public SolarImageToTextureConversion(SolarImageCoordinateSystem solarImageCoordinateSystem, TextureCoordinateSystem textureCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.textureCoordinateSystem = textureCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarImageCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

        // Relative Coordinates within Region
        double _x = (x - this.textureCoordinateSystem.getRegion().getCornerX()) / textureCoordinateSystem.getRegion().getWidth();
        double _y = (y - this.textureCoordinateSystem.getRegion().getCornerY()) / textureCoordinateSystem.getRegion().getHeight();

        CoordinateVector textureCoordinate = textureCoordinateSystem.createCoordinateVector(_x, _y);

        return textureCoordinate;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.solarImageCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.textureCoordinateSystem;
    }
}
