package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;

@SuppressWarnings("serial")
class SpaceObjectModel extends AbstractTableModel {

    private final ArrayList<SpaceObjectElement> elements = new ArrayList<>();

    SpaceObjectModel() {
        for (SpaceObject object : SpaceObject.getObjects())
            elements.add(new SpaceObjectElement(object, this));
    }

    List<SpaceObjectElement> getSelected() {
        ArrayList<SpaceObjectElement> selected = new ArrayList<>();
        for (SpaceObjectElement element : elements)
            if (element.isSelected())
                selected.add(element);
        return selected;
    }

    @Nullable
    SpaceObjectElement elementOf(SpaceObject object) {
        for (SpaceObjectElement element : elements) {
            if (element.getObject() == object)
                return element;
        }
        return null;
    }

    void refresh(SpaceObjectElement element) {
        int idx = elements.indexOf(element);
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
