package org.helioviewer.gl3d.wcs.impl;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.gl3d.wcs.Cartesian2DCoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.GenericCoordinateDimension;
import org.helioviewer.gl3d.wcs.Unit;
import org.helioviewer.viewmodel.region.Region;

/**
 * The {@link TextureCoordinateSystem} defines texture coordinates of an image
 * that is stored in a GL Texture. It also provides the Region that is actually
 * captured by the texture.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class TextureCoordinateSystem extends Cartesian2DCoordinateSystem {
    private Region region;
    private Vector2dDouble textureScale;

    public TextureCoordinateSystem(Vector2dDouble textureScale, Region region) {
        super(new GenericCoordinateDimension(Unit.Pixel, "Texture XCoordinate", 0, 1.0), new GenericCoordinateDimension(Unit.Pixel, "Texture YCoordinate", 0, 1.0));

        this.region = region;
        this.textureScale = textureScale;
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        return super.getConversion(coordinateSystem);
    }

    public Region getRegion() {
        return region;
    }

    public double getTextureScaleX() {
        return this.textureScale.getX();
    }

    public double getTextureScaleY() {
        return this.textureScale.getY();
    }
}
