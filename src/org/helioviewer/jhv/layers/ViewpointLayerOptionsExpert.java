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
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointLayerOptionsExpert {

    public static final int MIN_SPEED_SPIRAL = 200;
    public static final int MAX_SPEED_SPIRAL = 2000;

    private final boolean exclusive;
    private final SpaceObjectContainer container;
    private final Map<SpaceObjectElement, PositionLoad> loads = new HashMap<>();
    private Runnable changeListener = () -> {};
    private Runnable skyChangeListener = () -> {};

    private boolean syncInterval = true;
    private long startTime = Player.getStartTime();
    private long endTime = Player.getEndTime();
    private int spiralSpeed = 500;
    private boolean spiral = false;
    private boolean relative = false;
    private boolean planets = true;
    private boolean stars = true;

    private Frame frame;

    ViewpointLayerOptionsExpert(JSONObject jo, Frame _frame, boolean _exclusive) {
        exclusive = _exclusive;
        frame = _frame;

        JSONArray ja = null;
        if (jo != null) {
            try {
                frame = Frame.valueOf(jo.optString("frame"));
            } catch (Exception ignore) {}
            relative = jo.optBoolean("relativeLongitude", relative);
            planets = jo.optBoolean("planets", planets);
            stars = jo.optBoolean("stars", stars);
            if (!exclusive) {
                spiral = jo.optBoolean("spiral", spiral);
                spiralSpeed = Math.clamp(jo.optInt("spiralSpeed", spiralSpeed), MIN_SPEED_SPIRAL, MAX_SPEED_SPIRAL);
            }
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
        SpaceObject.getTargets(SpaceObject.SUN).forEach(target -> elements.add(new SpaceObjectElement(target)));

        container = new SpaceObjectContainer(elements, exclusive);
        selectTargets(ja);
    }

    void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    void setSkyChangeListener(Runnable listener) {
        skyChangeListener = listener;
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
        changeListener.run();
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
        skyChangeListener.run();
        changeListener.run();
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        container.setHighlightedElement(element);
        changeListener.run();
    }

    public boolean isSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(boolean _syncInterval) {
        syncInterval = _syncInterval;
        if (syncInterval)
            setTimespan(Player.getStartTime(), Player.getEndTime());
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
        changeListener.run();
    }

    public void setRelative(boolean _relative) {
        if (relative == _relative)
            return;

        relative = _relative;
        changeListener.run();
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

    JSONObject toJson() {
        JSONObject jo = new JSONObject();
        jo.put("frame", frame);
        jo.put("relativeLongitude", relative);
        if (exclusive) {
            jo.put("planets", planets);
            jo.put("stars", stars);
        } else {
            jo.put("spiral", spiral);
            jo.put("spiralSpeed", spiralSpeed);
        }
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
        ArrayList<PositionLoad> selectedLoads = new ArrayList<>();
        for (SpaceObjectElement element : container.getSelectedElements()) {
            PositionLoad load = loads.get(element);
            if (load != null)
                selectedLoads.add(load);
        }
        selectedLoads.removeIf(PositionLoad::hasFailed);
        return selectedLoads;
    }

    public boolean isPlanets() {
        return planets;
    }

    public void setPlanets(boolean _planets) {
        planets = _planets;
        skyChangeListener.run();
        DisplayController.display();
    }

    public boolean isStars() {
        return stars;
    }

    public void setStars(boolean _stars) {
        stars = _stars;
        skyChangeListener.run();
        DisplayController.display();
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
        skyChangeListener.run();
    }

    private void load(SpaceObjectElement element) {
        unload(element);

        loads.put(element, PositionLoad.submit(status -> {
            container.setStatus(element, status);
            DisplayController.refreshCamera();
        }, SpaceObject.SUN, element.getTarget(), frame, startTime, endTime));
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
