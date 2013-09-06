package org.helioviewer.viewmodel.view;

import org.helioviewer.viewmodel.metadata.MetaData;

/**
 * View to manage meta data.
 * 
 * <p>
 * This View manages meta data, so it is responsible for generating and updating
 * the meta data of the image.
 * 
 * <p>
 * Usually, the implementation of this interface generates new images, so
 * usually it is also either a {@link ImageInfoView} or a {@link LayeredView}.
 * 
 * <p>
 * Note, that it is expected to have at least one MetaDataView in every path of
 * the view chain. To take care of this requirement, implement the
 * {@link ImageInfoView} as recommended.
 * 
 * <p>
 * For further information about meta data, see
 * {@link org.helioviewer.viewmodel.metadata}
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface MetaDataView extends View {

    /**
     * Returns the meta data the image.
     * 
     * @return Meta data of the image
     */
    public MetaData getMetaData();
}
