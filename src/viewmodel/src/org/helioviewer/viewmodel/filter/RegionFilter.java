package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.region.Region;

/**
 * Filter which also receives the region of the image.
 * 
 * <p>
 * Some filters also need the region of the image in addition to the currently
 * visible part of the image. To receive this information, filters have to
 * implement this interface. The
 * {@link org.helioviewer.viewmodel.view.FilterView} will recognize this and
 * provide the information.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface RegionFilter extends Filter {

    /**
     * Sets the region of the image.
     * 
     * Usually, this function will be called by the
     * {@link org.helioviewer.viewmodel.view.FilterView}
     * 
     * @param region
     *            Region of the image.
     */
    public void setRegion(Region region);

}
