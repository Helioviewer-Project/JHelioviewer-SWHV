package org.helioviewer.jhv.position;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.threads.CancelTask;

public class LoadPositionSet {

    private static final HashMap<String, LoadPosition> loadPositionSet = new HashMap<>();

    public static LoadPosition add(StatusReceiver receiver, SpaceObject observer, SpaceObject target, Frame frame, long start, long end) {
        String url = LoadPosition.toUrl(observer, target, frame, start, end);
        LoadPosition load = loadPositionSet.get(url);

        if (load == null || load.isFailed()) {
            load = new LoadPosition(receiver, target, url);
            loadPositionSet.put(url, load);
            JHVGlobals.getExecutorService().execute(load);
            JHVGlobals.getReaperService().schedule(new CancelTask(load), 120, TimeUnit.SECONDS);
        } else
            receiver.setStatus("Loaded");
        return load;
    }

    void remove(LoadPosition loadPosition) {
        if (loadPosition != null)
            loadPositionSet.remove(loadPosition);
    }

}
