package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.helioviewer.jhv.app.Log;
import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.PositionResponse;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.Stars;
import org.helioviewer.jhv.display.DisplayController;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectContainer;
import org.helioviewer.jhv.layers.spaceobject.SpaceObjectElement;
import org.helioviewer.jhv.movie.Player;
import org.helioviewer.jhv.thread.AppThread;
import org.helioviewer.jhv.thread.Task;
import org.helioviewer.jhv.time.JHVTime;
import org.helioviewer.jhv.time.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ViewpointLayerOptionsExpert {

    public static final int MIN_SPEED_SPIRAL = 200;
    public static final int MAX_SPEED_SPIRAL = 2000;
    private static final String[] APPARENT_PLANETS = {"Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"};

    private final boolean exclusive;
    private final SpaceObject observer;
    private final SpaceObjectContainer container;
    private final Map<SpaceObjectElement, PositionLoad> loads = new HashMap<>();
    private final List<PositionLoad> apparentPlanets = new ArrayList<>();
    private PositionLoad apparentSun;
    private PositionLoad locationState;
    private PositionLoad locationHeliocentric;
    private final List<PositionLoad> locationLoads = new ArrayList<>(); // all of the above
    private Future<Stars.Catalog> starCatalog;
    private Runnable changeListener = () -> {};

    private boolean syncInterval = true;
    private long startTime = Player.getStartTime();
    private long endTime = Player.getEndTime();
    private int spiralSpeed = 500;
    private boolean spiral = false;
    private boolean relative = false;
    private boolean planets = true;
    private boolean stars = true;
    private boolean skyActive;

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
        ArrayList<PositionLoad> loads = new ArrayList<>();
        for (SpaceObjectElement element : container.getSelectedElements()) {
            PositionLoad load = this.loads.get(element);
            if (load != null)
                loads.add(load);
        }
        loads.removeIf(PositionLoad::hasFailed);
        return loads;
    }

    // only once the location itself, the viewpoint, is loaded
    @Nullable
    PositionResponse getApparentSun() {
        PositionLoad location = getHighlightedLoad();
        return location == null || location.getResponse() == null || apparentSun == null ? null : apparentSun.getResponse();
    }

    List<PositionLoad> getApparentPlanets() {
        return apparentPlanets;
    }

    @Nullable
    PositionResponse getLocationState() {
        return locationState == null ? null : locationState.getResponse();
    }

    @Nullable
    PositionResponse getLocationHeliocentric() {
        return locationHeliocentric == null ? null : locationHeliocentric.getResponse();
    }

    @Nullable
    Stars.Catalog getStarCatalog() {
        return starCatalog != null && starCatalog.state() == Future.State.SUCCESS ? starCatalog.resultNow() : null;
    }

    public boolean isPlanets() {
        return planets;
    }

    public void setPlanets(boolean _planets) {
        planets = _planets;
        reloadSky();
        DisplayController.display();
    }

    public boolean isStars() {
        return stars;
    }

    public void setStars(boolean _stars) {
        stars = _stars;
        reloadSky();
        DisplayController.display();
    }

    void setSkyActive(boolean _skyActive) {
        if (skyActive == _skyActive)
            return;
        skyActive = _skyActive;
        reloadSky();
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
        }, observer, target, frame, "NONE", startTime, endTime));
        reloadSky();
    }

    // apparent directions seen from the location in J2000, and its barycentric state and heliocentric position for the stars
    private void reloadSky() {
        cancelSky();
        updateStarCatalog();
        List<SpaceObjectElement> selected = container.getSelectedElements();
        if (!exclusive || !skyActive || !(planets || stars) || selected.isEmpty())
            return;
        SpaceObject location = SpaceObject.get(selected.getFirst().toString());
        if (location == null)
            return;

        PositionLoad.StatusReceiver receiver = status -> DisplayController.display();
        apparentSun = PositionLoad.submit(receiver, location, SpaceObject.SUN, Frame.J2000, "LT+S", startTime, endTime);
        locationLoads.add(apparentSun);
        if (planets) {
            for (String name : APPARENT_PLANETS) {
                SpaceObject planet = SpaceObject.get(name);
                if (planet != null && planet != location)
                    apparentPlanets.add(PositionLoad.submit(receiver, location, planet, Frame.J2000, "LT+S", startTime, endTime));
            }
            locationLoads.addAll(apparentPlanets);
        }
        if (stars) {
            locationState = PositionLoad.submitBarycentricState(receiver, location, startTime, endTime);
            locationHeliocentric = PositionLoad.submit(receiver, SpaceObject.SUN, location, Frame.J2000, "NONE", startTime, endTime);
            locationLoads.add(locationState);
            locationLoads.add(locationHeliocentric);
        }
    }

    private void cancelSky() {
        locationLoads.forEach(PositionLoad::cancel);
        locationLoads.clear();
        locationState = null;
        locationHeliocentric = null;
        apparentSun = null;
        apparentPlanets.clear();
    }

    // independent of time and location, kept while the stars are shown
    private void updateStarCatalog() {
        if (exclusive && skyActive && stars) {
            if (starCatalog == null)
                starCatalog = Task.submitBackground("stars", Stars::load, catalog -> DisplayController.display(), (logContext, t) -> {
                    if (!AppThread.isInterrupted(t))
                        Log.error(t);
                });
        } else if (starCatalog != null) {
            starCatalog.cancel(true);
            starCatalog = null;
        }
    }

    private void unload(SpaceObjectElement element) {
        cancelSky();

        PositionLoad load = loads.remove(element);
        if (load == null)
            return;

        load.cancel();
        container.setStatus(element, null);
        DisplayController.display();
    }

}
