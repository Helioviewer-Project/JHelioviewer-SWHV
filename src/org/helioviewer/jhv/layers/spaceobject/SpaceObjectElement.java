package org.helioviewer.jhv.layers.spaceobject;

import java.util.concurrent.TimeUnit;

import javax.swing.border.Border;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.position.LoadPosition;
import org.helioviewer.jhv.position.LoadPositionFire;
import org.helioviewer.jhv.threads.CancelTask;

class SpaceObjectElement implements LoadPositionFire {

    private final SpaceObject observer;
    private final SpaceObject target;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private LoadPosition load;

    SpaceObjectElement(SpaceObject _observer, SpaceObject _target, SpaceObjectModel _model) {
        observer = _observer;
        target = _target;
        model = _model;
    }

    void load(UpdateViewpoint uv, Frame frame, long startTime, long endTime) {
        selected = true;

        if (load != null) {
            load.cancel(true);
            uv.unsetLoadPosition(load);
            fireLoaded(null);
        }

        load = new LoadPosition(this, observer, target, frame, startTime, endTime);
        uv.setLoadPosition(load);
        JHVGlobals.getExecutorService().execute(load);
        JHVGlobals.getReaperService().schedule(new CancelTask(load), 120, TimeUnit.SECONDS);
    }

    void unload(UpdateViewpoint uv) {
        selected = false;

        if (load != null) {
            load.cancel(true);
            uv.unsetLoadPosition(load);
            fireLoaded(null);

            load = null;
            Display.display();
        }
    }

    boolean isTarget(SpaceObject _target) {
        return target.equals(_target);
    }

    boolean isDownloading() {
        return load != null && !load.isDone();
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
    public void fireLoaded(String _status) {
        status = _status;
        model.refresh(this);
        Display.getCamera().refresh();
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
