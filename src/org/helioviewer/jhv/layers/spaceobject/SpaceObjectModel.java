package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import org.helioviewer.jhv.astronomy.SpaceObject;

final class SpaceObjectModel {

    private final List<SpaceObjectElement> elements = new ArrayList<>();
    private final List<IntConsumer> refreshListeners = new ArrayList<>();
    private final Map<SpaceObject, Integer> targetRows = new HashMap<>();
    private final Map<SpaceObjectElement, Integer> elementRows = new HashMap<>();

    SpaceObjectModel(SpaceObject observer) {
        SpaceObject.getTargets(observer).forEach(target -> {
            SpaceObjectElement element = new SpaceObjectElement(target, this);
            int row = elements.size();
            elements.add(element);
            targetRows.put(target, row);
            elementRows.put(element, row);
        });
    }

    void forEachSelected(Consumer<SpaceObjectElement> action) {
        for (SpaceObjectElement element : elements) {
            if (element.isSelected())
                action.accept(element);
        }
    }

    boolean anySelected(Predicate<SpaceObjectElement> predicate) {
        for (SpaceObjectElement element : elements) {
            if (element.isSelected() && predicate.test(element))
                return true;
        }
        return false;
    }

    public int indexOf(SpaceObject object) {
        return targetRows.getOrDefault(object, -1);
    }

    public int indexOf(SpaceObjectElement element) {
        return elementRows.getOrDefault(element, -1);
    }

    public SpaceObjectElement elementAt(int row) {
        return elements.get(row);
    }

    public int size() {
        return elements.size();
    }

    public void addRefreshListener(IntConsumer listener) {
        refreshListeners.add(listener);
    }

    void refresh(SpaceObjectElement element) {
        int row = elementRows.getOrDefault(element, -1);
        if (row != -1)
            refreshListeners.forEach(listener -> listener.accept(row));
    }

}
