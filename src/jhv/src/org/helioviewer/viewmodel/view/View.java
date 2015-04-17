package org.helioviewer.viewmodel.view;

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

}
