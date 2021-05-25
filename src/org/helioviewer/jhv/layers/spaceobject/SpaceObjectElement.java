package org.helioviewer.jhv.layers.spaceobject;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.StatusReceiver;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.layers.MovieDisplay;

class SpaceObjectElement implements StatusReceiver {

    private final SpaceObject target;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private PositionLoad load;

    SpaceObjectElement(SpaceObject _target, SpaceObjectModel _model) {
        target = _target;
        model = _model;
    }

    void load(UpdateViewpoint uv, SpaceObject observer, Frame frame, long startTime, long endTime) {
        selected = true;

        if (load != null) {
            PositionLoad.remove(uv, load);
        }
        load = PositionLoad.submit(uv, this, observer, target, frame, startTime, endTime);
    }

    void unload(UpdateViewpoint uv) {
        selected = false;

        if (load != null) {
            PositionLoad.remove(uv, load);
            load = null;
            MovieDisplay.display();
        }
    }

    boolean isTarget(SpaceObject _target) {
        return target.equals(_target);
    }

    boolean isDownloading() {
        return load != null && load.isDownloading();
    }

    boolean isSelected() {
        return selected;
    }

    @Nullable
    PositionLoad getLoad(UpdateViewpoint uv) {
        List<PositionLoad> loads = PositionLoad.get(uv);
        int idx = loads.indexOf(load);
        return idx < 0 ? null : loads.get(idx);
    }

    String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String _status) {
        status = _status;
        model.refresh(this);
        Display.getCamera().refresh();
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
