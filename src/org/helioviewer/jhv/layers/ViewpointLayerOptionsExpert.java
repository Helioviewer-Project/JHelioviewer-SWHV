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
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectModel;
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
    private final Map<SpaceObject, SpaceObjectElement> targetElements = new HashMap<>();
    private final Map<SpaceObjectElement, SpaceObject> elementTargets = new HashMap<>();
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

        JSONArray selectedTargets = ja;
        ArrayList<SpaceObjectElement> elements = new ArrayList<>();
        SpaceObject.getTargets(observer).forEach(target -> {
            SpaceObjectElement element = new SpaceObjectElement(target.toString());
            elements.add(element);
            targetElements.put(target, element);
            elementTargets.put(element, target);
        });

        container = new SpaceObjectContainer(new SpaceObjectModel(elements), exclusive);
        selectTargets(selectedTargets);
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
        if (exclusive) {
            for (SpaceObjectElement selectedElement : selected) {
                if (selectedElement != element)
                    unload(selectedElement);
            }
            if (!wasSelected)
                load(element);
        } else if (wasSelected) {
            unload(element);
        } else {
            load(element);
        }
        if (!exclusive || !wasSelected)
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
        jo.put("objects", container.toJson());
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
        loads.removeIf(load -> load.future().isCancelled() || (load.future().isDone() && load.getResponse() == null));
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
            SpaceObjectElement element = targetElements.get(SpaceObject.get(selectedTargets.optString(i, "Earth")));
            if (element != null)
                selectElement(element);
        }
    }

    private void reloadSelected() {
        for (SpaceObjectElement element : container.getSelectedElements())
            load(element);
    }

    private void load(SpaceObjectElement element) {
        unload(element);

        SpaceObject target = elementTargets.get(element);
        if (target == null)
            return;

        loads.put(element, PositionLoad.submit(status -> {
            element.setStatus(status);
            DisplayController.refreshCamera();
        }, observer, target, frame, startTime, endTime));
    }

    private void unload(SpaceObjectElement element) {
        PositionLoad load = loads.remove(element);
        if (load == null)
            return;

        PositionLoad.remove(load);
        element.setStatus(null);
        DisplayController.display();
    }

}
