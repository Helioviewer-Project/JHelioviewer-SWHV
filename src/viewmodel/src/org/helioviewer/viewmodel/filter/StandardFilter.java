package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.imagedata.ImageData;

/**
 * Filter which supports filtering in software rendering mode.
 * 
 * <p>
 * Although OpenGL accelerated are much faster, every filter should provide a
 * pure software implementation as a fallback solution. Even when using the
 * OpenGL rendering mode (which is not possible on all machines) it is not
 * guaranteed that a filter actually will be performed in OpenGL.
 * 
 * <p>
 * In software rendering mode, the filter gets the current image data objects
 * and has to return a new one containing the filtered data.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface StandardFilter extends Filter {

    /**
     * Applies the filter to the image data.
     * 
     * @param data
     *            Input data
     * @return Filtered output data
     */
    public ImageData apply(ImageData data);

}
