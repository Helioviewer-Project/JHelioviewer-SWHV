package org.helioviewer.jhv.io;

import java.io.IOException;
import java.net.URI;

import org.helioviewer.base.logging.Log;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.fitsview.JHVFITSView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2CallistoView;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.simpleimageview.JHVSimpleImageView;

// package visibility
final class LoadView {

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
