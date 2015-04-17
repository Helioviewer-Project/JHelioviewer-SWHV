package org.helioviewer.viewmodel.view.jp2view;

import org.helioviewer.viewmodel.view.View;

/**
 * View controlling an JPG2000 image
 * 
 * <p>
 * This view provides the capability to control the number of quality layers
 * used.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface JP2View extends View {

    /**
     * Returns the number of quality layers currently used.
     * 
     * @return Number of quality layers currently used
     */
    public int getCurrentNumQualityLayers();

    /**
     * Returns the maximal number of quality layers.
     * 
     * @return Maximal number of quality layers
     */
    public int getMaximumNumQualityLayers();

    /**
     * Sets the number of quality layers that shall be used.
     * 
     * @param newNumQualityLayers
     *            Number of quality layers that shall be used
     */
    public void setNumQualityLayers(int newNumQualityLayers);

}
