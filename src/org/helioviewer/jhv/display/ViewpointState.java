package org.helioviewer.jhv.display;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.time.JHVTime;

final class ViewpointState {

    private boolean tracking;
    private Position viewpoint = Sun.StartEarth;
    private UpdateViewpoint updateViewpoint;

    ViewpointState(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    Position update(JHVTime time) {
        viewpoint = updateViewpoint.update(time);
        return viewpoint;
    }

    Position getViewpoint() {
        return viewpoint;
    }

    UpdateViewpoint getUpdateViewpoint() {
        return updateViewpoint;
    }

    void setUpdateViewpoint(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    boolean setTrackingMode(boolean _tracking) {
        if (tracking == _tracking)
            return false;

        tracking = _tracking;
        return true;
    }

    boolean getTrackingMode() {
        return tracking;
    }
}
