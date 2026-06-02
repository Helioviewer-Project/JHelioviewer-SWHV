package org.helioviewer.jhv.layers.spaceobject;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.display.DisplayController;

public final class SpaceObjectElement implements PositionLoad.StatusReceiver {

    private final SpaceObject target;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private PositionLoad load;

    SpaceObjectElement(SpaceObject _target, SpaceObjectModel _model) {
        target = _target;
        model = _model;
    }

    void load(SpaceObject observer, Frame frame, long startTime, long endTime) {
        selected = true;
        status = null;
        model.refresh(this);

        if (load != null) {
            PositionLoad.remove(load);
        }
        load = PositionLoad.submit(this, observer, target, frame, startTime, endTime);
    }

    void unload() {
        selected = false;
        status = null;
        model.refresh(this);

        if (load != null) {
            PositionLoad.remove(load);
            load = null;
            DisplayController.display();
        }
    }

    boolean isDownloading() {
        return load != null && load.isDownloading();
    }

    public boolean isSelected() {
        return selected;
    }

    @Nullable
    public PositionLoad getLoad() {
        return load;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String _status) {
        status = _status;
        model.refresh(this);
        DisplayController.refreshCamera();
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
