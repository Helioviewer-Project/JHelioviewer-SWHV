package org.helioviewer.gl3d.changeevent;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.view.View;

/**
 * ChangedReason when the GL Component was resized.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class ComponentViewSizeChangedReason implements ChangedReason {

    private View sender;

    private Vector2dInt imagePanelSize;

    public ComponentViewSizeChangedReason(View sender, Vector2dInt imagePanelSize) {
        this.sender = sender;
        this.imagePanelSize = imagePanelSize;
    }

    public View getView() {
        return sender;
    }

    public Vector2dInt getImagePanelSize() {
        return imagePanelSize;
    }
}
