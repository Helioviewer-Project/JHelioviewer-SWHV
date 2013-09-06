package org.helioviewer.gl3d.changeevent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.viewmodel.changeevent.ChangedReason;
import org.helioviewer.viewmodel.view.View;

/**
 * ChangedReason when the active {@link GL3DCamera} has changed.
 * 
 * @author Simon Spšrri (simon.spoerri@fhnw.ch)
 * 
 */
public class CameraChangeChangedReason implements ChangedReason {

    private View sender;

    private GL3DCamera camera;

    public CameraChangeChangedReason(View sender, GL3DCamera newCamera) {
        this.sender = sender;
        this.camera = newCamera;
    }

    public View getView() {
        return sender;
    }

    public GL3DCamera getCamera() {
        return this.camera;
    }

}
