package org.helioviewer.jhv.camera.object;

import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.io.LoadPosition;
import org.helioviewer.jhv.io.LoadPositionFire;
import org.helioviewer.jhv.threads.CancelTask;

class SpaceObjectElement implements LoadPositionFire {

    private final SpaceObject object;
    private final SpaceObjectModel model;

    private boolean selected;
    private String status;
    private LoadPosition load;

    SpaceObjectElement(SpaceObject _object, SpaceObjectModel _model) {
        object = _object;
        model = _model;
    }

    SpaceObject getObject() {
        return object;
    }

    void load(UpdateViewpoint uv, String frame, long startTime, long endTime) {
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

    boolean isDownloading() {
        return load == null ? false : !load.isLoaded();
    }

    boolean isSelected() {
        return selected;
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
        return object.toString();
    }

}
