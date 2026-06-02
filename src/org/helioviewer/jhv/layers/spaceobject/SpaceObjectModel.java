package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class SpaceObjectModel {

    private final List<SpaceObjectElement> elements = new ArrayList<>();
    private final List<IntConsumer> refreshListeners = new ArrayList<>();
    private final Map<SpaceObjectElement, Integer> elementRows = new HashMap<>();

    public SpaceObjectModel(List<SpaceObjectElement> _elements) {
        for (SpaceObjectElement element : _elements) {
            int row = elements.size();
            elements.add(element);
            elementRows.put(element, row);
            element.setModel(this);
        }
    }

    void forEachSelected(Consumer<SpaceObjectElement> action) {
        for (SpaceObjectElement element : elements) {
            if (element.isSelected())
                action.accept(element);
        }
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
