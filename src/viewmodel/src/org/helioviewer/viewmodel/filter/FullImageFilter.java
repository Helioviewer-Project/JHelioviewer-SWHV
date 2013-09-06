package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.imagedata.ImageData;

/*
 * TODO: Implement the support for FullFilterImage
 */

/**
 * Filter which receives always the full image additionally to the (partial)
 * input image data.
 * 
 * <p>
 * Some filters always may need the full image data in addition to the currently
 * visible part of the image. To receive this information, filters have to
 * implement this interface. The
 * {@link org.helioviewer.viewmodel.view.FilterView} will recognize this and
 * provide the information.
 * 
 * <p>
 * <b>NOTE: THIS INTERFACE IS NOT SUPPORTED YET!</b>
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface FullImageFilter extends Filter {

    /**
     * Sets the full image.
     * 
     * Usually, this function will be called by the
     * {@link org.helioviewer.viewmodel.view.FilterView}
     * 
     * @param fullImageData
     *            Image data object containing the full image
     */
    public void setFullImage(ImageData fullImageData);

}
