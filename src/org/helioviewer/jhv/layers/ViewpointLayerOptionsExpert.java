package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointLayerOptionsExpert {

    public static final int MIN_SPEED_SPIRAL = 200;
    public static final int MAX_SPEED_SPIRAL = 2000;

    private final boolean exclusive;
    private final SpaceObjectContainer container;
    private Runnable changeListener = () -> {};

    private boolean syncInterval = true;
    private long startTime = Movie.getStartTime();
    private long endTime = Movie.getEndTime();
    private int spiralSpeed = 500;
    private boolean spiral = false;
    private boolean relative = false;

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, SpaceObject observer, Frame _frame, boolean _exclusive) {
        exclusive = _exclusive;
        frame = _frame;

        JSONArray ja = null;
        if (jo != null) {
            try {
                frame = Frame.valueOf(jo.optString("frame"));
            } catch (Exception ignore) {}
            relative = jo.optBoolean("relativeLongitude", relative);
            ja = jo.optJSONArray("objects");
            syncInterval = jo.optBoolean("syncInterval", syncInterval);
            if (!syncInterval) {
                startTime = TimeUtils.optParse(jo.optString("startTime"), startTime);
                endTime = TimeUtils.optParse(jo.optString("endTime"), endTime);
            }
        }
        if (ja == null)
            ja = new JSONArray(new String[]{"Earth"});

        container = new SpaceObjectContainer(ja, exclusive, observer, frame, startTime, endTime);
        container.setChangeListener(this::fireChanged);
    }

    void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    private void fireChanged() {
        changeListener.run();
    }

    public void setTimespan(long start, long end) {
        if (!syncInterval)
            return;
        setTimeSelection(start, end);
    }

    public void setTimeSelection(long start, long end) {
        startTime = start;
        endTime = end;
        container.setTime(start, end);
    }

    public boolean hasFrameOptions() {
        return !exclusive;
    }

    public SpaceObjectContainer getContainer() {
        return container;
    }

    public boolean isSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(boolean _syncInterval) {
        syncInterval = _syncInterval;
        if (syncInterval)
            setTimespan(Movie.getStartTime(), Movie.getEndTime());
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame _frame) {
        frame = _frame;
        container.setFrame(frame);
    }

    public void setRelative(boolean _relative) {
        if (relative == _relative)
            return;

        relative = _relative;
        fireChanged();
    }

    public int getSpiralSpeedValue() {
        return spiralSpeed;
    }

    public void setSpiralSpeed(int _spiralSpeed) {
        spiralSpeed = _spiralSpeed;
        DisplayController.display();
    }

    public boolean isSpiral() {
        return spiral;
    }

    public void setSpiral(boolean _spiral) {
        spiral = _spiral;
        DisplayController.display();
    }

    boolean isDownloading() {
        return container.isDownloading();
    }

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("frame", frame);
        jo.put("relativeLongitude", relative);
        jo.put("syncInterval", syncInterval);
        if (!syncInterval) {
            jo.put("startTime", new JHVTime(startTime));
            jo.put("endTime", new JHVTime(endTime));
        }
        jo.put("objects", container.toJson());
        return jo;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        SpaceObjectElement element = container.getHighlightedElement();
        return element == null ? null : element.getLoad();
    }

    List<PositionLoad> getSelectedLoads() {
        ArrayList<PositionLoad> loads = new ArrayList<>();
        for (SpaceObjectElement element : container.getSelectedElements()) {
            PositionLoad load = element.getLoad();
            if (load != null)
                loads.add(load);
        }
        loads.removeIf(load -> load.future().isCancelled() || (load.future().isDone() && load.getResponse() == null));
        return loads;
    }

    int getSpiralSpeed() {
        return spiral ? spiralSpeed : 0;
    }

    public boolean isRelative() {
        return relative;
    }

}
