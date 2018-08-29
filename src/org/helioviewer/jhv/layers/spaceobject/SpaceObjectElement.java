package org.helioviewer.jhv.layers.spaceobject;

import javax.swing.border.Border;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.position.LoadPosition;
import org.helioviewer.jhv.position.StatusReceiver;

class SpaceObjectElement implements StatusReceiver {

    private final SpaceObject target;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private LoadPosition load;

    SpaceObjectElement(SpaceObject _target, SpaceObjectModel _model) {
        target = _target;
        model = _model;
    }

    void load(UpdateViewpoint uv, SpaceObject observer, Frame frame, long startTime, long endTime) {
        selected = true;

        if (load != null) {
            uv.removeLoader(load);
        }
        load = LoadPosition.execute(this, observer, target, frame, startTime, endTime);
        uv.addLoader(load);
    }

    void unload(UpdateViewpoint uv) {
        selected = false;

        if (load != null) {
            uv.removeLoader(load);
            load = null;
            Display.display();
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

    Border getBorder() {
        return target.getBorder();
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
