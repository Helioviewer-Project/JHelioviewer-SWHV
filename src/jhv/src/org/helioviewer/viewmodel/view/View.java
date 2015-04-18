package org.helioviewer.viewmodel.view;

import java.net.URI;

import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;

public interface View {

    /**
     * Returns the image data produced by the view
     *
     * @return produced image data
     */
    public ImageData getSubimageData();

    /**
     * Sets the current region.
     *
     * @param r
     *            The new region
     * @return True, if the region has changed, false otherwise
     * @see #getRegion
     */
    public boolean setRegion(Region r);

    /**
     * Returns the current region.
     *
     * @return Current region
     * @see #setRegion
     */
    public Region getRegion();

    /**
     * Returns the meta data the image.
     *
     * @return Meta data of the image
     */
    public MetaData getMetaData();
=======
import org.helioviewer.base.interval.Interval;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * View to manage an image data source.
 *
 * <p>
 * This view represents undermost view within the view chain. It should not be
 * possible to use another view as an input for this view.
 *
 * <p>
 * Usually, it is expected to have at least one {@link RegionView} per path
 * within the view chain, so it might be a good idea to always implement them
 * together with the ImageInfoView, since every path starts at an ImageInfoView.
 *
 * @author Ludwig Schmidt
 */
public interface ImageInfoView {
>>>>>>> create abstraction:jhv-3d-wcs/src/jhv/src/org/helioviewer/viewmodel/view/ImageInfoView.java

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    public URI getUri();

    /**
     * Returns the name the image.
     *
     * This might be the filename, but it can be something else extracted from
     * the meta data.
     *
     * @return Name of the image
     */
    public String getName();

    /**
     * Returns, whether the image is a remote image (e.g. jpip).
     *
     * @return true, if the image is accessed remotely, false otherwise
     */
    public boolean isRemote();

    /**
     * Returns the download uri the image.
     *
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     *
     * @return download uri
     */
    public URI getDownloadURI();

    public Interval<Date> getDateRange();

    public boolean getBaseDifferenceMode();

    public boolean getDifferenceMode();

    public ImageData getBaseDifferenceImageData();

    public ImageData getPreviousImageData();

    public ImageData getImageData();

    public ImageData getSubimageData();

    public MetaData getMetaData();

    public boolean setRegion(Region r);

    public Region getRegion();

    public boolean setViewport(Viewport r);

}
