package org.helioviewer.jhv.layers.spaceobject;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Frame;
import org.helioviewer.jhv.astronomy.PositionLoad;
import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.astronomy.UpdateViewpoint;

import org.json.JSONArray;

public final class SpaceObjectContainer {

    private final boolean exclusive;
    private final UpdateViewpoint uv;
    private final SpaceObject observer;
    private final SpaceObjectModel model;

    private SpaceObjectElement highlighted;
    private Frame frame;
    private long startTime;
    private long endTime;

    public SpaceObjectContainer(JSONArray ja, boolean _exclusive, UpdateViewpoint _uv, SpaceObject _observer, Frame _frame, long _startTime, long _endTime) {
        exclusive = _exclusive;
        uv = _uv;
        observer = _observer;
        frame = _frame;
        startTime = _startTime;
        endTime = _endTime;

        model = new SpaceObjectModel(observer);

        PositionLoad.removeAll(uv);

        int len = ja.length();
        for (int i = 0; i < len; i++)
            selectTarget(SpaceObject.get(ja.optString(i, "Earth")));
    }

    public SpaceObjectModel getModel() {
        return model;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    @Nullable
    public SpaceObjectElement getHighlightedElement() {
        return highlighted;
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        highlighted = element;
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
        return highlighted == null ? null : highlighted.getLoad(uv);
    }

    public void setFrame(Frame _frame) {
        if (frame == _frame)
            return;

        frame = _frame;
        model.forEachSelected(element -> element.load(uv, observer, frame, startTime, endTime));
    }

    public void setTime(long _startTime, long _endTime) {
        if (startTime == _startTime && endTime == _endTime)
            return;

        startTime = _startTime;
        endTime = _endTime;
        model.forEachSelected(element -> element.load(uv, observer, frame, startTime, endTime));
    }

    public void selectElement(SpaceObjectElement element) {
        highlighted = element;
        if (exclusive) {
            if (element.isSelected()) // avoid reload on re-clicking same
                return;
            model.forEachSelected(e -> e.unload(uv));
            element.load(uv, observer, frame, startTime, endTime);
        } else {
            if (element.isSelected())
                element.unload(uv);
            else
                element.load(uv, observer, frame, startTime, endTime);
        }
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
