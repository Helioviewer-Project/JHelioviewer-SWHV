package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.image.ImageGrid;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.metadata.Region;

// Complete buffer-pixel <-> WCS-plane mapping. Pixel Y follows buffer rows.
// The original geometry owns the image footprint; a decoded grid composes with it.
public final class ImageGeometry {

    public final ImageGeometry original;
    public final ImageGrid grid;
    public final WcsHeader wcs;
    public final Mat2 pixelToPlane;
    private final Mat2 planeToPixel;
    public final Vec2 referencePixel;
    public final double pixelSizeX;
    public final double pixelSizeY;
    public final Vec2 sunPixel;
    public final Region bounds;

    // Complete WCS-plane -> normalized buffer coordinates, ready for rendering.
    public final Mat2 planeToTexture;
    public final Vec2 referenceTexture;

    public ImageGeometry(int width, int height, double pixelSize) {
        this(width, height, new WcsHeader(WcsHeader.Projection.TAN, new float[6], Sun.MeanEarthDistance, Vec2.ZERO),
                new Mat2(pixelSize, 0, 0, -pixelSize), new Vec2(.5 * width, .5 * height));
    }

    public ImageGeometry(int width, int height, WcsHeader _wcs, Mat2 _pixelToPlane, Vec2 _referencePixel) {
        this(null, new ImageGrid(0, width, height), _wcs, _pixelToPlane, _referencePixel);
    }

    private ImageGeometry(ImageGeometry source, ImageGrid _grid, WcsHeader _wcs, Mat2 _pixelToPlane, Vec2 _referencePixel) {
        original = source == null ? this : source;
        grid = _grid;
        wcs = _wcs;
        pixelToPlane = _pixelToPlane;
        planeToPixel = pixelToPlane.inverse();
        referencePixel = _referencePixel;
        pixelSizeX = Math.hypot(pixelToPlane.m00, pixelToPlane.m10);
        pixelSizeY = Math.hypot(pixelToPlane.m01, pixelToPlane.m11);
        bounds = new Region(-referencePixel.x * pixelSizeX, -(grid.height() - referencePixel.y) * pixelSizeY,
                grid.width() * pixelSizeX, grid.height() * pixelSizeY);
        if (source != null) {
            double step = grid.step();
            sunPixel = new Vec2((source.sunPixel.x - grid.firstX()) / step + .5, (source.sunPixel.y - grid.firstY()) / step + .5);
        } else {
            Vec2 sun = !wcs.projection.isSurfaceMap() && (wcs.crval.x != 0 || wcs.crval.y != 0)
                    ? WcsProjection.helioprojectiveToPlane(wcs, 0, 0) : null;
            sunPixel = sun == null ? referencePixel : toPixel(sun.x, sun.y);
        }
        planeToTexture = new Mat2(planeToPixel.m00 / grid.width(), planeToPixel.m01 / grid.width(),
                planeToPixel.m10 / grid.height(), planeToPixel.m11 / grid.height());
        referenceTexture = new Vec2(referencePixel.x / grid.width(), referencePixel.y / grid.height());
    }

    // Actual source dimensions can differ from NAXIS; the WCS reference pixel stays fixed.
    public ImageGeometry withSize(int width, int height) {
        return width == grid.width() && height == grid.height() ? this : new ImageGeometry(width, height, wcs, pixelToPlane, referencePixel);
    }

    public ImageGeometry forGrid(ImageGrid decoded) {
        ImageGeometry source = original;
        if (source.grid.equals(decoded))
            return source;
        double step = decoded.step();
        Mat2 transform = new Mat2(source.pixelToPlane.m00 * step, source.pixelToPlane.m01 * step,
                source.pixelToPlane.m10 * step, source.pixelToPlane.m11 * step);
        Vec2 reference = new Vec2((source.referencePixel.x - decoded.firstX()) / step + .5,
                (source.referencePixel.y - decoded.firstY()) / step + .5);
        return new ImageGeometry(source, decoded, source.wcs, transform, reference);
    }

    public Vec2 toPlane(double x, double y) {
        return pixelToPlane.transform(x - referencePixel.x, y - referencePixel.y);
    }

    public Vec2 toPixel(double x, double y) {
        Vec2 offset = planeToPixel.transform(x, y);
        return new Vec2(offset.x + referencePixel.x, offset.y + referencePixel.y);
    }
}
