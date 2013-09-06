package org.helioviewer.viewmodel.changeevent;

import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the non constant part of the meta data
 * object has changed.
 * 
 * For further information about non constant meta data, see
 * {@link org.helioviewer.viewmodel.metadata.NonConstantMetaData}.
 * 
 * @author Markus Langenberg
 * */
public class NonConstantMetaDataChangedReason implements ChangedReason {

    // memorizes the associated view
    private View view;

    // memorizes the changed meta data
    private MetaData metaData;

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     */
    public NonConstantMetaDataChangedReason(View aView, MetaData aNewMetaData) {

        // memorize view
        view = aView;

        metaData = aNewMetaData;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new meta data object which was defined when this change
     * reason occurred.
     * 
     * @return new meta data object.
     * */
    public MetaData getNewMetaData() {
        return metaData;
    }
}
