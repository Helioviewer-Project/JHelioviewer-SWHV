package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.imagedata.ImageData;

/**
 * View for providing image data.
 * 
 * <p>
 * This view provides the actual image data. Any view, which shall change the
 * content of the image, has to implement this interface. Within the view chain,
 * the image data is passed from view to view by using a an ImageData object.
 * 
 * <p>
 * For further information about image data, see
 * {@link org.helioviewer.viewmodel.imagedata}
 * 
 * @author Ludwig Schmidt
 */
public interface SubimageDataView extends View {

    /**
     * Returns the image data produced by the view
     * 
     * @return produced image data
     */
    public ImageData getSubimageData();

}
