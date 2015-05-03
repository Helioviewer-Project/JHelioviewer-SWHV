package org.helioviewer.viewmodel.view;

import java.net.URI;

import org.helioviewer.base.Region;
import org.helioviewer.base.Viewport;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.MetaData;

/**
 * View to manage an image data source.
 *
 * @author Ludwig Schmidt
 */
public interface View {
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
