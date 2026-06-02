package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import javax.annotation.Nullable;

import org.json.JSONArray;

public final class SpaceObjectContainer {

    private final boolean exclusive;
    private final SpaceObjectModel model;

    private SpaceObjectElement highlighted;

    public SpaceObjectContainer(SpaceObjectModel _model, boolean _exclusive) {
        exclusive = _exclusive;
        model = _model;
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

    public boolean isExclusive() {
        return exclusive;
    }

    public int getHighlightedIndex() {
        return highlighted == null ? -1 : model.indexOf(highlighted);
    }

    public void setHighlightedElement(SpaceObjectElement element) {
        highlighted = element;
    }

    @Nullable
    public SpaceObjectElement getHighlightedElement() {
        return highlighted;
    }

    public List<SpaceObjectElement> getSelectedElements() {
        ArrayList<SpaceObjectElement> elements = new ArrayList<>();
        model.forEachSelected(elements::add);
        return elements;
    }

    public void selectElement(SpaceObjectElement element) {
        highlighted = element;
        if (exclusive) {
            if (element.isSelected()) // avoid reload on re-clicking same
                return;
            model.forEachSelected(SpaceObjectElement::deselect);
            element.select();
        } else {
            if (element.isSelected())
                element.deselect();
            else
                element.select();
        }
    }

    public JSONArray toJson() {
        JSONArray ja = new JSONArray();
        model.forEachSelected(ja::put);
        return ja;
    }

}
