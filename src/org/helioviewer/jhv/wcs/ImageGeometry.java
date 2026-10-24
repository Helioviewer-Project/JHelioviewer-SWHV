package org.helioviewer.jhv.wcs;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.math.Mat2;
import org.helioviewer.jhv.math.Region;
import org.helioviewer.jhv.math.Vec2;

// Complete buffer-pixel <-> WCS-plane mapping. Pixel Y follows buffer rows.
// The original geometry owns the image footprint; a decoded grid composes with it.
public final class ImageGeometry {

    private static final WcsSolution PLAIN_IMAGE_WCS = new WcsSolution(WcsSolution.Projection.TAN, new float[6], Sun.MeanEarthDistance);

    public final ImageGeometry original;
    public final ImageGrid grid;
    public final WcsSolution wcs;
    public final Mat2 pixelToPlane;
    public final Mat2 planeToPixel;
    public final Vec2 referencePixel;
    public final double pixelSizeX;
    public final double pixelSizeY;
    public final Vec2 sunPixel;
    public final Region bounds; // Original image footprint, independent of the decoded grid.

    private volatile Adjustment adjustment;

    private record Adjustment(double longitude, double latitude, WcsSolution wcs) {}

    public ImageGeometry(int width, int height, double pixelSize) {
        this(width, height, PLAIN_IMAGE_WCS,
                new Mat2(pixelSize, 0, 0, -pixelSize), new Vec2(.5 * width, .5 * height));
    }

    // Pixel centers: FITS = integers starting at 1, OpenGL = half-integers starting at 0.5.
    // JHV buffers have their origin at the upper-left edge, with Y following buffer rows.
    // Convert FITS centers and upward Y once: x = CRPIX1 - 0.5, y = height - (CRPIX2 - 0.5).
    public ImageGeometry(int width, int height, WcsSolution _wcs, boolean pixelsAlreadyRotated) {
        Mat2 transform = _wcs.pixelToPlane;
        if (pixelsAlreadyRotated)
            transform = new Mat2(Math.hypot(transform.m00, transform.m10), 0, 0, Math.hypot(transform.m01, transform.m11));
        this(width, height, _wcs, new Mat2(transform.m00, -transform.m01, transform.m10, -transform.m11),
                new Vec2(_wcs.referencePixel.x - .5, height - (_wcs.referencePixel.y - .5)));
    }

    public ImageGeometry(int width, int height, WcsSolution _wcs, Mat2 _pixelToPlane, Vec2 _referencePixel) {
        this(null, new ImageGrid(0, width, height), _wcs, _pixelToPlane, _referencePixel);
    }

    private ImageGeometry(ImageGeometry source, ImageGrid _grid, WcsSolution _wcs, Mat2 _pixelToPlane, Vec2 _referencePixel) {
        original = source == null ? this : source;
        grid = _grid;
        wcs = _wcs;
        pixelToPlane = _pixelToPlane;
        planeToPixel = pixelToPlane.inverse();
        referencePixel = _referencePixel;
        pixelSizeX = Math.hypot(pixelToPlane.m00, pixelToPlane.m10);
        pixelSizeY = Math.hypot(pixelToPlane.m01, pixelToPlane.m11);
        bounds = source == null ? new Region(-referencePixel.x * pixelSizeX, -(grid.height() - referencePixel.y) * pixelSizeY,
                grid.width() * pixelSizeX, grid.height() * pixelSizeY) : source.bounds;
        if (source != null) {
            double step = grid.step();
            sunPixel = new Vec2((source.sunPixel.x - grid.firstX()) / step + .5, (source.sunPixel.y - grid.firstY()) / step + .5);
        } else {
            Vec2 sun = !wcs.projection.isSurfaceMap() ? wcs.worldToPlane(0, 0) : null;
            sunPixel = sun == null ? referencePixel : toPixel(sun.x, sun.y);
        }
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

    // Cache adjustments per original image; decoded grids reuse them even when base WCS solutions are shared.
    public WcsSolution adjustedWcs(double longitudeArcsec, double latitudeArcsec) {
        ImageGeometry source = original;
        if (longitudeArcsec == 0 && latitudeArcsec == 0)
            return source.wcs;
        Adjustment cached = source.adjustment;
        if (cached != null && cached.longitude == longitudeArcsec && cached.latitude == latitudeArcsec)
            return cached.wcs;
        WcsSolution result = source.wcs.adjusted(longitudeArcsec, latitudeArcsec);
        source.adjustment = new Adjustment(longitudeArcsec, latitudeArcsec, result);
        return result;
    }

    // Observer images use their recorded viewpoint; surface maps use the displayed viewpoint.
    public Position tracingViewpoint(Position display, Position source) {
        return wcs.projection.isSurfaceMap() ? display : source;
    }

    public Vec2 toPlane(double x, double y) {
        return pixelToPlane.transform(x - referencePixel.x, y - referencePixel.y);
    }

    public Vec2 toPixel(double x, double y) {
        Vec2 offset = planeToPixel.transform(x, y);
        return new Vec2(offset.x + referencePixel.x, offset.y + referencePixel.y);
    }
}
