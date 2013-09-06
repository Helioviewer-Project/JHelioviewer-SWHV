package org.helioviewer.jhv.gui.components.statusplugins;

import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.view.View;

/**
 * Class represents a change reason when the 'pointer' to the layer currently
 * being active has changed.
 * 
 * <p>
 * This reason is used for the graphical user interface only, since the view
 * chain itself does not define an active layer.
 * <p>
 * In the current flow of development, LayersModel introduces the concept of an
 * active layer By systematically changing the code to using the
 * LayersModel-Class & the LayersListener, this ChangedReason should not be
 * needed anymore.
 * <p>
 * The current idea behind is, that ChangedReasons should only be used within
 * the ViewChain
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 */
@Deprecated
public class ActiveLayerChangedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param view
     *            View which caused the change reason
     */
    public ActiveLayerChangedReason(View view) {
        this.view = view;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

}
