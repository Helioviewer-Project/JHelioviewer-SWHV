package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.region.Region;

/**
 * View to manage the region of the image currently shown.
 * 
 * <p>
 * This view manages the region, which is currently displayed. By accessing this
 * view, changing the region is also possible.
 * 
 * <p>
 * Note, that the region is not necessarily specified in pixels. Instead, the
 * region should be given in an unit, that allows calculating the location and
 * size of different images in relation to each other correctly. Since a pixel
 * might be referring to an physical area, whose real physical size might be
 * different from image to image, solar images usually specify their region in
 * kilometers.
 * 
 * <p>
 * Also note, that it is expected to have at least one RegionView in every path
 * of the view chain. To take care of this requirement, implement the
 * {@link ImageInfoView} as recommended.
 * 
 * <p>
 * For further informations about regions, also see
 * {@link org.helioviewer.viewmodel.region}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface RegionView extends View {

    /**
     * Sets the current region.
     * 
     * @param r
     *            The new region
     * @param event
     *            ChangeEvent to append all changes following
     * @return True, if the region has changed, false otherwise
     * @see #getRegion
     */
    public boolean setRegion(Region r, ChangeEvent event);

    /**
     * Returns the current region.
     * 
     * @return Current region
     * @see #setRegion
     */
    public Region getRegion();
}
