package org.helioviewer.jhv.camera.object;

import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.camera.LoadPosition;
import org.helioviewer.jhv.camera.LoadPositionFire;
import org.helioviewer.jhv.camera.UpdateViewpoint;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.threads.CancelTask;

public class SpaceObjectElement implements LoadPositionFire {

    private final SpaceObject object;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private LoadPosition load;

    public SpaceObjectElement(SpaceObject _object, SpaceObjectModel _model) {
        object = _object;
        model = _model;
    }

    public SpaceObject getObject() {
        return object;
    }

    public void select(UpdateViewpoint uv, String frame, long startTime, long endTime) {
        selected = true;

        if (load != null) {
            load.cancel(true);
            uv.unsetLoadPosition(load);
            fireLoaded(null);
        }

        load = new LoadPosition(this, object, frame, startTime, endTime);
        uv.setLoadPosition(load);
        JHVGlobals.getExecutorService().execute(load);
        JHVGlobals.getReaperService().schedule(new CancelTask(load), 120, TimeUnit.SECONDS);
    }

    public void deselect(UpdateViewpoint uv) {
        selected = false;

        if (load != null) {
            load.cancel(true);
            uv.unsetLoadPosition(load);
            fireLoaded(null);

            load = null;
            Displayer.display();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void fireLoaded(String _status) {
        status = _status;
        model.refresh(this);
        Displayer.getCamera().refresh();
    }

    @Override
    public String toString() {
        return object.toString();
    }

}
