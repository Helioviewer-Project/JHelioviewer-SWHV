package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;

import org.json.JSONArray;

public final class SpaceObjectContainer {

    private final boolean exclusive;
    private final SpaceObject observer;
    private final SpaceObjectModel model;
    private Runnable changeListener = () -> {};

    private SpaceObjectElement highlighted;
    private Frame frame;
    private long startTime;
    private long endTime;

    public SpaceObjectContainer(JSONArray ja, boolean _exclusive, SpaceObject _observer, Frame _frame, long _startTime, long _endTime) {
        exclusive = _exclusive;
        observer = _observer;
        frame = _frame;
        startTime = _startTime;
        endTime = _endTime;

        model = new SpaceObjectModel(observer);

        int len = ja.length();
        for (int i = 0; i < len; i++)
            selectTarget(SpaceObject.get(ja.optString(i, "Earth")));
    }

    public int size() {
        return model.size();
    }

    public SpaceObjectElement elementAt(int row) {
        return model.elementAt(row);
    }

    public void addRefreshListener(IntConsumer listener) {
        model.addRefreshListener(listener);
    }

    public void setChangeListener(Runnable listener) {
        changeListener = listener;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public int getHighlightedIndex() {
        return highlighted == null ? -1 : model.indexOf(highlighted);
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        highlighted = element;
        changeListener.run();
    }

    private void selectTarget(SpaceObject target) {
        if (target == null)
            return;
        int idx = model.indexOf(target);
        if (idx != -1) { // found
            SpaceObjectElement element = model.elementAt(idx);
            selectElement(element);
        }
    }

    @Nullable
    public PositionLoad getHighlightedLoad() {
        return highlighted == null ? null : highlighted.getLoad();
    }

    public List<PositionLoad> getSelectedLoads() {
        ArrayList<PositionLoad> loads = new ArrayList<>();
        model.forEachSelected(element -> {
            PositionLoad load = element.getLoad();
            if (load != null)
                loads.add(load);
        });
        loads.removeIf(load -> load.future().isCancelled() || (load.future().isDone() && load.getResponse() == null));
        return loads;
    }

    public void setFrame(Frame _frame) {
        if (frame == _frame)
            return;

        frame = _frame;
        model.forEachSelected(element -> element.load(observer, frame, startTime, endTime));
        changeListener.run();
    }

    public void setTime(long _startTime, long _endTime) {
        if (startTime == _startTime && endTime == _endTime)
            return;

        startTime = _startTime;
        endTime = _endTime;
        model.forEachSelected(element -> element.load(observer, frame, startTime, endTime));
        changeListener.run();
    }

    public void selectElement(SpaceObjectElement element) {
        highlighted = element;
        if (exclusive) {
            if (element.isSelected()) // avoid reload on re-clicking same
                return;
            model.forEachSelected(SpaceObjectElement::unload);
            element.load(observer, frame, startTime, endTime);
        } else {
            if (element.isSelected())
                element.unload();
            else
                element.load(observer, frame, startTime, endTime);
        }
        changeListener.run();
    }

    public boolean isDownloading() {
        return model.anySelected(SpaceObjectElement::isDownloading);
    }

    public JSONArray toJson() {
        JSONArray ja = new JSONArray();
        model.forEachSelected(ja::put);
        return ja;
    }

}
