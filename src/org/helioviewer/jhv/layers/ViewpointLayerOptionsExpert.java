package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.movie.Movie;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointLayerOptionsExpert {

    public static final int MIN_SPEED_SPIRAL = 200;
    public static final int MAX_SPEED_SPIRAL = 2000;

    private final boolean exclusive;
    private final SpaceObject observer;
    private final SpaceObjectContainer container;
    private final Map<SpaceObjectElement, PositionLoad> loads = new HashMap<>();
    private Runnable changeListener = () -> {};

    private boolean syncInterval = true;
    private long startTime = Movie.getStartTime();
    private long endTime = Movie.getEndTime();
    private int spiralSpeed = 500;
    private boolean spiral = false;
    private boolean relative = false;

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, SpaceObject _observer, Frame _frame, boolean _exclusive) {
        exclusive = _exclusive;
        observer = _observer;
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

        ArrayList<SpaceObjectElement> elements = new ArrayList<>();
        SpaceObject.getTargets(observer).forEach(target -> elements.add(new SpaceObjectElement(target.toString())));

        container = new SpaceObjectContainer(elements, exclusive);
        selectTargets(ja);
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
        if (startTime == start && endTime == end)
            return;

        startTime = start;
        endTime = end;
        reloadSelected();
        fireChanged();
    }

    public boolean hasFrameOptions() {
        return !exclusive;
    }

    public SpaceObjectContainer getContainer() {
        return container;
    }

    public void selectElement(SpaceObjectElement element) {
        List<SpaceObjectElement> selected = container.getSelectedElements();
        boolean wasSelected = element.isSelected();
        container.selectElement(element);

        if (wasSelected) {
            if (exclusive)
                return;
            unload(element);
        } else {
            if (exclusive)
                selected.forEach(this::unload);
            load(element);
        }
        fireChanged();
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        container.setHighlightedElement(element);
        fireChanged();
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
        if (frame == _frame)
            return;

        frame = _frame;
        reloadSelected();
        fireChanged();
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
        for (PositionLoad load : loads.values()) {
            if (load.isDownloading())
                return true;
        }
        return false;
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
        JSONArray objects = new JSONArray();
        for (SpaceObjectElement element : container.getSelectedElements())
            objects.put(element);
        jo.put("objects", objects);
        return jo;
    }

    @Nullable
    PositionLoad getHighlightedLoad() {
        SpaceObjectElement element = container.getHighlightedElement();
        return element == null ? null : loads.get(element);
    }

    List<PositionLoad> getSelectedLoads() {
        ArrayList<PositionLoad> loads = new ArrayList<>();
        for (SpaceObjectElement element : container.getSelectedElements()) {
            PositionLoad load = this.loads.get(element);
            if (load != null)
                loads.add(load);
        }
        loads.removeIf(PositionLoad::hasFailed);
        return loads;
    }

    int getSpiralSpeed() {
        return spiral ? spiralSpeed : 0;
    }

    public boolean isRelative() {
        return relative;
    }

    private void selectTargets(JSONArray selectedTargets) {
        int len = selectedTargets.length();
        for (int i = 0; i < len; i++) {
            String target = selectedTargets.optString(i, "Earth");
            for (SpaceObjectElement element : container.getElements()) {
                if (element.toString().equals(target)) {
                    selectElement(element);
                    break;
                }
            }
        }
    }

    private void reloadSelected() {
        for (SpaceObjectElement element : container.getSelectedElements())
            load(element);
    }

    private void load(SpaceObjectElement element) {
        unload(element);

        SpaceObject target = SpaceObject.get(element.toString());
        if (target == null)
            return;

        loads.put(element, PositionLoad.submit(status -> {
            container.setStatus(element, status);
            DisplayController.refreshCamera();
        }, observer, target, frame, startTime, endTime));
    }

    private void unload(SpaceObjectElement element) {
        PositionLoad load = loads.remove(element);
        if (load == null)
            return;

        load.cancel();
        container.setStatus(element, null);
        DisplayController.display();
    }

}
