package org.helioviewer.viewmodel.filter;

import org.helioviewer.viewmodel.metadata.MetaData;

/**
 * Filter which also receives the meta data of the image.
 * 
 * <p>
 * Some filters also need the meta data of the image in addition to the
 * currently visible part of the image. To receive this information, filters
 * have to implement this interface. The
 * {@link org.helioviewer.viewmodel.view.FilterView} will recognize this and
 * provide the information.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface MetaDataFilter extends Filter {

    /**
     * Sets the meta data of the image.
     * 
     * Usually, this function will be called by the
     * {@link org.helioviewer.viewmodel.view.FilterView}
     * 
     * @param metaData
     *            Meta data of the image
     */
    public void setMetaData(MetaData metaData);

}
