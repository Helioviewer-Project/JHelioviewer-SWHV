package org.helioviewer.viewmodel.view;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractList;
import java.util.Date;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXCallistoView;
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

        if (Math.abs(r.getWidth() / r.getHeight() - viewportRatio) > JavaCompatibility.DOUBLE_MIN_NORMAL * 4) {
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

        if (Math.abs(r.getWidth() / r.getHeight() - viewportRatio) > JavaCompatibility.DOUBLE_MIN_NORMAL * 4) {
            return cropRegionToImage(StaticRegion.createAdaptedRegion(r.getRectangle().contractToAspectRatioKeepingCenter(viewportRatio)),
                    m);
        } else {
            return r;
        }
    }

    /**
     * Returns a View of given interface or class, starting search at given
     * view.
     * 
     * If the given view implements the given interface itself, it returns that
     * very same view, otherwise it returns a suitable view (for example another
     * view located deeper within the view chain, that can provide the desired
     * information, or null, if that is not possible).
     * 
     * @param <T>
     *            Subclass of {@link View}
     * @param v
     *            First view to analyze
     * @param c
     *            Class or interface to search for
     * @return View implementing given class or interface, if available, null
     *         otherwise
     */
    public static <T extends View> T getViewAdapter(View v, Class<T> c) {
        return v == null ? null : v.getAdapter(c);
    }

    /**
     * Returns an ImageData object of given class or interface.
     * 
     * <p>
     * The function searches the next {@link SubimageDataView}, fetches its
     * ImageData object and tests, whether it satisfies the given class or
     * interface. If so, it returns the ImageData object, otherwise, it returns
     * null
     * 
     * @param <T>
     *            Subclass of
     *            {@link org.helioviewer.viewmodel.imagedata.ImageData}
     * @param v
     *            First view to analyze
     * @param c
     *            Class or interface to search for
     * @return ImageData implementing given class or interface, if available,
     *         null otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T extends ImageData> T getImageDataAdapter(View v, Class<T> c) {

        SubimageDataView dataView = getViewAdapter(v, SubimageDataView.class);

        if (dataView == null) {
            return null;

        } else if (dataView.getSubimageData() == null) {
            return null;
        } else if (!c.isInstance(dataView.getSubimageData())) {
            return null;
        } else {
            return (T) dataView.getSubimageData();
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
    public static ViewportImageSize calculateViewportImageSize(Viewport v, Region r) {
        if (v == null || r == null) {
            return null;
        }

        double screenMeterPerPixel;
        double screenSubImageWidth;
        double screenSubImageHeight;

        // fit region of interest into viewport
        if ((double) v.getWidth() / (double) v.getHeight() > r.getWidth() / r.getHeight()) {
            screenMeterPerPixel = r.getHeight() / v.getHeight();
            screenSubImageHeight = v.getHeight();
            screenSubImageWidth = r.getWidth() / screenMeterPerPixel;
        } else {
            screenMeterPerPixel = r.getWidth() / v.getWidth();
            screenSubImageWidth = v.getWidth();
            screenSubImageHeight = r.getHeight() / screenMeterPerPixel;
        }

        return StaticViewportImageSize.createAdaptedViewportImageSize((int) Math.round(screenSubImageWidth),
                (int) Math.round(screenSubImageHeight));
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
     * <p>
     * Basically, this function fetches the region and viewport of the given
     * view and calls {@link #calculateViewportImageSize(Viewport, Region)}.
     * 
     * @param v
     *            View containing the image.
     * @return resulting image size of the region within the viewport
     */
    public static ViewportImageSize calculateViewportImageSize(View v) {
        RegionView regionView = ViewHelper.getViewAdapter(v, RegionView.class);
        ViewportView viewportView = ViewHelper.getViewAdapter(v, ViewportView.class);
        if (regionView == null || viewportView == null) {
            return null;
        }
        return calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion());
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
    public static Vector2dDouble convertScreenToImageDisplacement(Vector2dInt screenDisplacement, Region r, ViewportImageSize v) {
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
    public static Vector2dDouble convertScreenToImageDisplacement(int screenDisplacementX, int screenDisplacementY, Region r,
            ViewportImageSize v) {
        return new Vector2dDouble(r.getWidth() / (v.getWidth()) * screenDisplacementX, -r.getHeight() / (v.getHeight())
                * screenDisplacementY);
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
    public static Vector2dInt convertImageToScreenDisplacement(Vector2dDouble imageDisplacement, Region r, ViewportImageSize v) {
        return convertImageToScreenDisplacement(imageDisplacement.getX(), imageDisplacement.getY(), r, v);
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
    public static Vector2dInt convertImageToScreenDisplacement(double imageDisplacementX, double imageDisplacementY, Region r,
            ViewportImageSize v) {
        return new Vector2dInt((int) Math.round(imageDisplacementX / r.getWidth() * v.getWidth()), (int) Math.round(imageDisplacementY
                / r.getHeight() * v.getHeight()));
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

        Vector2dDouble halfSize = r.getSize().scale(0.5);
        Vector2dDouble oldCenter = r.getLowerLeftCorner().add(halfSize);
        Vector2dDouble newCenter = oldCenter.crop(m.getPhysicalLowerLeft(), m.getPhysicalUpperRight());

        if (oldCenter.equals(newCenter)) {
            return r;
        }

        return StaticRegion.createAdaptedRegion(newCenter.subtract(halfSize), r.getSize());
    }

    /**
     * Ensures, that the given inner region is within the given outer region.
     * 
     * If that is not the case, crops the inner region to the outer region.
     * 
     * @param innerRegion
     *            Inner region to crop, if necessary
     * @param outerRegion
     *            Outer region, defining the maximal bounds
     * @return region located inside the outer region
     */
    public static Region cropInnerRegionToOuterRegion(Region innerRegion, Region outerRegion) {
        return StaticRegion.createAdaptedRegion(innerRegion.getRectangle().cropToOuterRectangle(outerRegion.getRectangle()));
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
        return ViewHelper.convertImageToScreenDisplacement(innerRegion.getUpperLeftCorner().subtract(outerRegion.getUpperLeftCorner()),
                outerRegion, outerViewportImageSize).negateY();
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
    public static ImageInfoView loadView(URI uri, Interval<Date> range) throws IOException {
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
    public static ImageInfoView loadView(URI uri, boolean isMainView, Interval<Date> range) throws IOException {
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
    public static ImageInfoView loadView(URI uri, URI downloadURI, Interval<Date> range) throws IOException {
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
    public static ImageInfoView loadView(URI uri, URI downloadURI, boolean isMainView, Interval<Date> range) throws IOException {
        if (uri == null || uri.getScheme() == null || uri.toString() == null) {
            throw new IOException("Invalid URI.");
        }

        String[] parts = uri.toString().split("\\.");
        String ending = parts[parts.length - 1];

        if (ending.equals("jpeg") || ending.equals("jpg") || ending.equals("JPEG") || ending.equals("JPG") ||
            ending.equals("png")  || ending.equals("PNG")) {
            return new JHVSimpleImageView(uri, null);

        } else if (ending.equals("fits") || ending.equals("FITS") || ending.equals("fts") || ending.equals("FTS")) {
            try {
                return new JHVFITSView(uri, null);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }

        } else if (downloadURI.toString().toLowerCase().contains("callisto")) {
            try {
                JP2Image jp2Image = new JP2Image(uri, downloadURI);

                if (jp2Image.isMultiFrame()) {
                    JHVJPXCallistoView jpxCallistoView = new JHVJPXCallistoView(isMainView, range, true);
                    jpxCallistoView.setJP2Image(jp2Image);
                    return jpxCallistoView;
                } else {
                    JHVJP2CallistoView jp2CallistoView = new JHVJP2CallistoView(isMainView, range);
                    jp2CallistoView.setJP2Image(jp2Image);
                    return jp2CallistoView;
                }
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        } else {
            try {
                JP2Image jp2Image = new JP2Image(uri, downloadURI);

                if (jp2Image.isMultiFrame()) {
                    JHVJPXView jpxView = new JHVJPXView(isMainView, range, true);
                    jpxView.setJP2Image(jp2Image);
                    return jpxView;
                } else {
                    JHVJP2View jp2View = new JHVJP2View(isMainView, range);
                    jp2View.setJP2Image(jp2Image);
                    return jp2View;
                }
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * Searches the direct successor of the LayeredView being a predecessor of
     * the given view. Therefore, this functions traverses recursively through
     * all the view listeners of the given view.
     * 
     * @param aView
     *            Starting view for the search
     * @return View being a direct successor of the LayeredView and a
     *         predecessor of the given view
     */
    public static View findLastViewBeforeLayeredView(View aView) {
        AbstractList<ViewListener> viewListeners = aView.getAllViewListeners();

        for (ViewListener v : viewListeners) {
            if (v instanceof LayeredView) {
                return aView;
            } else {
                if (v instanceof View) {
                    View result = findLastViewBeforeLayeredView((View) v);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

}
