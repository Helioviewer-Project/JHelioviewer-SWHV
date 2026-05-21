package org.helioviewer.jhv.camera;

import java.util.HashSet;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.time.JHVTime;

public final class ViewpointModel {

    public interface Listener {
        void viewpointChanged(Position v);
    }

    private final HashSet<Listener> listeners = new HashSet<>();

    private boolean tracking;
    private Position viewpoint = Sun.StartEarth;
    private UpdateViewpoint updateViewpoint;

    public ViewpointModel(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.viewpointChanged(viewpoint);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    Position update(JHVTime time) {
        viewpoint = updateViewpoint.update(time);
        // listeners.forEach(l -> l.viewpointChanged(viewpoint));
        return viewpoint;
    }

    public Position getViewpoint() {
        return viewpoint;
    }

    public UpdateViewpoint getUpdateViewpoint() {
        return updateViewpoint;
    }

    public void setUpdateViewpoint(UpdateViewpoint _updateViewpoint) {
        updateViewpoint = _updateViewpoint;
    }

    public boolean setTrackingMode(boolean _tracking) {
        if (tracking == _tracking)
            return false;

        tracking = _tracking;
        return true;
    }

    public boolean getTrackingMode() {
        return tracking;
    }
}
