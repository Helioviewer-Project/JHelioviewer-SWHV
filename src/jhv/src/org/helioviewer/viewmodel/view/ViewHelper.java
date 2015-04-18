package org.helioviewer.viewmodel.view;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Collection of useful functions for use within the view chain.
 *
 * <p>
 * This class provides many different helpful functions, covering topics such as
 * scaling and alignment of regions, navigation within the view chain and
 * loading new images
 *
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 *
 */
public final class ViewHelper {

    /**
     * Expands the aspect ratio of the given region to the given viewport.
     *
     * <p>
     * When a region is resized, it usually does not fit in the viewport without
     * distorting it. To prevent unused areas or deformed images, this function
     * expands the region to fit the viewport. This might not be possible, if
     * the maximum of the region, given in the meta data, is reached.
     *
     * <p>
     * Note, that the region is always expanded, never cropped.
     *
     * <p>
     * Also note, that if the aspect ration already is equal, the given region
     * is returned.
     *
     * @param v
     *            Target viewport, which the region should fit in
     * @param r
     *            Source region, which has to be expanded
     * @param m
     *            Meta data of the image, to read maximal region
     * @return Expanded region
     * @see #contractRegionToViewportAspectRatio(Viewport, Region, MetaData)
     */
    public static Region expandRegionToViewportAspectRatio(Viewport v, Region r, MetaData m) {

        if (v == null) {
            return r;
        }

        double viewportRatio = v.getAspectRatio();

        if (Math.abs(r.getWidth() / r.getHeight() - viewportRatio) > Double.MIN_NORMAL * 4) {
            return cropRegionToImage(StaticRegion.createAdaptedRegion(r.getRectangle().expandToAspectRatioKeepingCenter(viewportRatio)), m);
        } else {
            return r;
        }
    }

    /**
     * Contracts the aspect ratio of the given region to the given viewport.
     *
     * <p>
     * When a region is resized, it usually does not fit in the viewport without
     * distorting it. To prevent unused areas or deformed images, this function
     * contracts the region to fit the viewport.
     *
     * <p>
     * Note, that if the aspect ration already is equal, the given region is
     * returned.
     *
     * @param v
     *            Target viewport, which the region should fit in
     * @param r
     *            Source region, which has to be contracted
     * @param m
     *            Meta data of the image, to read maximal region
     * @return Contracted region
     * @see #expandRegionToViewportAspectRatio(Viewport, Region, MetaData)
     */
    public static Region contractRegionToViewportAspectRatio(Viewport v, Region r, MetaData m) {

        double viewportRatio = v.getAspectRatio();

        if (Math.abs(r.getWidth() / r.getHeight() - viewportRatio) > Double.MIN_NORMAL * 4) {
            return cropRegionToImage(StaticRegion.createAdaptedRegion(r.getRectangle().contractToAspectRatioKeepingCenter(viewportRatio)), m);
        } else {
            return r;
        }
    }

    /**
     * Calculates the final size of a given region within the viewport.
     *
     * <p>
     * The resulting size is smaller or equal to the size of the viewport. It is
     * equal if and only if the aspect ratio of the region is equal to the
     * aspect ratio of the viewport. Otherwise, the image size is cropped to
     * keep the regions aspect ratio and not deform the image.
     *
     * @param v
     *            viewport, in which the image will be displayed
     * @param r
     *            visible region of the image
     * @return resulting image size of the region within the viewport
     */
    public static ViewportImageSize calculateViewportImageSize(Region r) {
        int viewportwidth;
        int viewportheight;
        if (GL3DState.get() == null) {
            viewportwidth = 512;
            viewportheight = 512;
        } else {
            viewportwidth = GL3DState.get().getViewportWidth();
            viewportheight = GL3DState.get().getViewportHeight();
        }
        double screenMeterPerPixel;
        double screenSubImageWidth;
        double screenSubImageHeight;
        // fit region of interest into viewport
        screenMeterPerPixel = r.getHeight() / viewportheight;
        screenSubImageHeight = viewportheight;
        screenSubImageWidth = r.getWidth() / screenMeterPerPixel;
        System.out.println("SIW" + screenSubImageWidth + " " + screenSubImageHeight);
        return StaticViewportImageSize.createAdaptedViewportImageSize((int) Math.round(screenSubImageWidth), (int) Math.round(screenSubImageHeight));
    }

    /**
     * Converts a given displacement on the screen to image coordinates.
     *
     * @param screenDisplacement
     *            Displacement on the screen to convert
     * @param r
     *            Region of the image currently visible on the screen
     * @param v
     *            ViewportImageSize of the image within the current viewport
     * @return Displacement in image coordinates
     */
    public static GL3DVec2d convertScreenToImageDisplacement(Vector2dInt screenDisplacement, Region r, ViewportImageSize v) {
        return convertScreenToImageDisplacement(screenDisplacement.getX(), screenDisplacement.getY(), r, v);
    }

    /**
     * Converts a given displacement on the screen to image coordinates.
     *
     * @param screenDisplacementX
     *            X-coordinate of the displacement on the screen to convert
     * @param screenDisplacementY
     *            Y-coordinate of the displacement on the screen to convert
     * @param r
     *            Region of the image currently visible on the screen
     * @param v
     *            ViewportImageSize of the image within the current viewport
     * @return Displacement in image coordinates
     */
    public static GL3DVec2d convertScreenToImageDisplacement(int screenDisplacementX, int screenDisplacementY, Region r, ViewportImageSize v) {
        return new GL3DVec2d(r.getWidth() / (v.getWidth()) * screenDisplacementX, -r.getHeight() / (v.getHeight()) * screenDisplacementY);
    }

    /**
     * Converts a given displacement on the image to screen coordinates.
     *
     * @param imageDisplacement
     *            Displacement on the image to convert
     * @param r
     *            Region of the image currently visible on the screen
     * @param v
     *            ViewportImageSize of the image within the current viewport
     * @return Displacement in screen coordinates
     */
    public static Vector2dInt convertImageToScreenDisplacement(GL3DVec2d imageDisplacement, Region r, ViewportImageSize v) {
        return convertImageToScreenDisplacement(imageDisplacement.x, imageDisplacement.y, r, v);
    }

    /**
     * Converts a given displacement on the image to screen coordinates.
     *
     * @param imageDisplacementX
     *            X-coordinate of the displacement on the image to convert
     * @param imageDisplacementY
     *            Y-coordinate of the displacement on the image to convert
     * @param r
     *            Region of the image currently visible on the screen
     * @param v
     *            ViewportImageSize of the image within the current viewport
     * @return Displacement in screen coordinates
     */
    public static Vector2dInt convertImageToScreenDisplacement(double imageDisplacementX, double imageDisplacementY, Region r, ViewportImageSize v) {
        return new Vector2dInt((int) Math.round(imageDisplacementX / r.getWidth() * v.getWidth()), (int) Math.round(imageDisplacementY / r.getHeight() * v.getHeight()));
    }

    /**
     * Ensures, that the given region is within the maximal bounds of the image
     * data.
     *
     * If that is not the case, moves and/or crops the region to the maximal
     * area given by the meta data.
     *
     * @param r
     *            Region to move and crop, if necessary
     * @param m
     *            Meta data defining the maximal region
     * @return Region located inside the maximal region
     */
    public static Region cropRegionToImage(Region r, MetaData m) {
        if (r == null || m == null) {
            return r;
        }

        GL3DVec2d halfSize = GL3DVec2d.scale(r.getSize(), 0.5);
        GL3DVec2d oldCenter = GL3DVec2d.add(r.getLowerLeftCorner(), halfSize);
        GL3DVec2d newCenter = GL3DVec2d.crop(oldCenter, m.getPhysicalLowerLeft(), m.getPhysicalUpperRight());

        if (oldCenter.equals(newCenter)) {
            return r;
        }

        return StaticRegion.createAdaptedRegion(GL3DVec2d.subtract(newCenter, halfSize), r.getSize());
    }

    /**
     * Calculates the inner viewport to the corresponding inner region.
     *
     * Given the outer region and the outer viewport image size, this function
     * calculates the part of the outer viewport image size, that is occupied by
     * the inner region.
     *
     * @param innerRegion
     *            inner region, whose inner viewport is requested
     * @param outerRegion
     *            outer region, as a reference
     * @param outerViewportImageSize
     *            outer viewport image size, as a reference
     * @return viewport corresponding to the inner region based on the outer
     *         region and viewport image size
     * @see #calculateInnerViewportOffset
     */
    public static Viewport calculateInnerViewport(Region innerRegion, Region outerRegion, ViewportImageSize outerViewportImageSize) {
        double newWidth = outerViewportImageSize.getWidth() * innerRegion.getWidth() / outerRegion.getWidth();
        double newHeight = outerViewportImageSize.getHeight() * innerRegion.getHeight() / outerRegion.getHeight();
        return StaticViewport.createAdaptedViewport((int) Math.round(newWidth), (int) Math.round(newHeight));
    }

    /**
     * Calculates the offset of the inner viewport relative to the outer
     * viewport image size.
     *
     * Given the outer region and viewport image size, calculates the offset of
     * the inner viewport corresponding to the given inner region.
     *
     * @param innerRegion
     *            inner region, whose inner viewport offset is requested
     * @param outerRegion
     *            outer region, as a reference
     * @param outerViewportImageSize
     *            outer viewport image size, as a reference
     * @return offset of the inner viewport based on the outer region and
     *         viewport image size
     * @see #calculateInnerViewport
     */
    public static Vector2dInt calculateInnerViewportOffset(Region innerRegion, Region outerRegion, ViewportImageSize outerViewportImageSize) {
        return ViewHelper.convertImageToScreenDisplacement(GL3DVec2d.subtract(innerRegion.getUpperLeftCorner(), outerRegion.getUpperLeftCorner()), outerRegion, outerViewportImageSize).negateY();
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the
     * ImageInfoView is chosen. If there is no implementation available for the
     * given type, an exception is thrown.
     *
     * <p>
     * Calls {@link #loadView(URI, boolean)} with the boolean set to true.
     *
     * @param uri
     *            URI representing the location of the image
     * @return ImageInfoView containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractImageInfoView loadView(URI uri) throws IOException {
        return loadView(uri, true, range);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the
     * ImageInfoView is chosen. If there is no implementation available for the
     * given type, an exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param isMainView
     *            Whether the view is used as a main view or not
     * @return ImageInfoView containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractImageInfoView loadView(URI uri, boolean isMainView) throws IOException {
        return loadView(uri, uri, isMainView, range);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the
     * ImageInfoView is chosen. If there is no implementation available for the
     * given type, an exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     *
     * @return ImageInfoView containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractImageInfoView loadView(URI uri, URI downloadURI) throws IOException {
        return loadView(uri, downloadURI, true, range);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the
     * ImageInfoView is chosen. If there is no implementation available for the
     * given type, an exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     * @param isMainView
     *            Whether the view is used as a main view or not
     * @return ImageInfoView containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractImageInfoView loadView(URI uri, URI downloadURI, boolean isMainView) throws IOException {
        if (uri == null || uri.getScheme() == null || uri.toString() == null) {
            throw new IOException("Invalid URI");
        }

        String[] parts = uri.toString().split("\\.");
        String ending = parts[parts.length - 1];
        if (downloadURI.toString().toLowerCase().endsWith(".fits") || downloadURI.toString().toLowerCase().endsWith(".fts")) {
            try {
                JHVFITSView fitsView = new JHVFITSView(uri, range);

                return fitsView;
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        } else if (downloadURI.toString().toLowerCase().contains("callisto")) {
            try {
                JP2Image jp2Image = new JP2Image(uri, downloadURI);
                JHVJP2CallistoView jp2CallistoView = new JHVJP2CallistoView(isMainView);

                jp2CallistoView.setJP2Image(jp2Image);
                return jp2CallistoView;
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        } else {
            try {
                JP2Image jp2Image = new JP2Image(uri, downloadURI);

                if (jp2Image.isMultiFrame()) {
                    JHVJPXView jpxView = new JHVJPXView(isMainView);
                    jpxView.setJP2Image(jp2Image);
                    return jpxView;
                } else {
                    JHVJP2View jp2View = new JHVJP2View(isMainView);
                    jp2View.setJP2Image(jp2Image);
                    return jp2View;
                }
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        }
    }

}
