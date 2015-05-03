package org.helioviewer.viewmodel.view;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.base.Viewport;
import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.simpleimageview.JHVSimpleImageView;

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
    public static GL3DVec2d convertScreenToImageDisplacement(Vector2dInt screenDisplacement, Region r, Vector2dInt viewportImageSize) {
        return convertScreenToImageDisplacement(screenDisplacement.getX(), screenDisplacement.getY(), r, viewportImageSize);
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
    public static GL3DVec2d convertScreenToImageDisplacement(int screenDisplacementX, int screenDisplacementY, Region r, Vector2dInt viewportImageSize) {
        return new GL3DVec2d(r.getWidth() / viewportImageSize.getX() * screenDisplacementX, -r.getHeight() / viewportImageSize.getY() * screenDisplacementY);
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
    public static Vector2dInt convertImageToScreenDisplacement(GL3DVec2d imageDisplacement, Region r, Vector2dInt viewportImageSize) {
        return convertImageToScreenDisplacement(imageDisplacement.x, imageDisplacement.y, r, viewportImageSize);
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
    public static Vector2dInt convertImageToScreenDisplacement(double imageDisplacementX, double imageDisplacementY, Region r, Vector2dInt viewportImageSize) {
        return new Vector2dInt((int) Math.round(imageDisplacementX / r.getWidth() * viewportImageSize.getX()), (int) Math.round(imageDisplacementY / r.getHeight() * viewportImageSize.getY()));
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
    public static Viewport calculateInnerViewport(Region innerRegion, Region outerRegion, Vector2dInt outerViewportImageSize) {
        double newWidth = outerViewportImageSize.getX() * innerRegion.getWidth() / outerRegion.getWidth();
        double newHeight = outerViewportImageSize.getY() * innerRegion.getHeight() / outerRegion.getHeight();
        return new Viewport((int) Math.round(newWidth), (int) Math.round(newHeight));
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
    public static Vector2dInt calculateInnerViewportOffset(Region innerRegion, Region outerRegion, Vector2dInt outerViewportImageSize) {
        return ViewHelper.convertImageToScreenDisplacement(GL3DVec2d.subtract(innerRegion.getUpperLeftCorner(), outerRegion.getUpperLeftCorner()), outerRegion, outerViewportImageSize).negateY();
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * <p>
     * Calls {@link #loadView(URI, boolean)} with the boolean set to true.
     *
     * @param uri
     *            URI representing the location of the image
     * @return View containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractView loadView(URI uri) throws IOException {
        return loadView(uri, true);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param isMainView
     *            Whether the view is used as a main view or not
     * @return View containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractView loadView(URI uri, boolean isMainView) throws IOException {
        return loadView(uri, uri, isMainView);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     *
     * @return View containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractView loadView(URI uri, URI downloadURI) throws IOException {
        return loadView(uri, downloadURI, true);
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     * @param isMainView
     *            Whether the view is used as a main view or not
     * @return View containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static AbstractView loadView(URI uri, URI downloadURI, boolean isMainView) throws IOException {
        if (uri == null || uri.getScheme() == null || uri.toString() == null) {
            throw new IOException("Invalid URI");
        }

        if (downloadURI.toString().toLowerCase().endsWith(".fits") || downloadURI.toString().toLowerCase().endsWith(".fts")) {
            try {
                JHVFITSView fitsView = new JHVFITSView(uri);

                return fitsView;
            } catch (Exception e) {
                Log.debug("ViewerHelper::loadView(\"" + uri + "\", \"" + downloadURI + "\", \"" + isMainView + "\") ", e);
                throw new IOException(e.getMessage());
            }
        } else if (downloadURI.toString().toLowerCase().endsWith(".png") || downloadURI.toString().toLowerCase().endsWith(".jpg") || downloadURI.toString().toLowerCase().endsWith(".jpeg")) {
            try {
                JHVSimpleImageView imView = new JHVSimpleImageView(uri);

                return imView;
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
