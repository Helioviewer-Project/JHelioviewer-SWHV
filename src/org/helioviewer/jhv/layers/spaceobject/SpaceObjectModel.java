package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;

@SuppressWarnings("serial")
class SpaceObjectModel extends AbstractTableModel {

    private final List<SpaceObjectElement> elements = new ArrayList<>();
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

    int indexOf(SpaceObject object) {
        return targetRows.getOrDefault(object, -1);
    }

    SpaceObjectElement elementAt(int row) {
        return elements.get(row);
    }

    void refresh(SpaceObjectElement element) {
        Integer idx = elementRows.get(element);
        if (idx >= 0)
            fireTableRowsUpdated(idx, idx);
    }

    @Override
    public int getRowCount() {
        return elements.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return elements.get(row);
    }

}
