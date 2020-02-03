package org.helioviewer.jhv.layers.spaceobject;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;

@SuppressWarnings("serial")
class SpaceObjectModel extends AbstractTableModel {

    private final ArrayList<SpaceObjectElement> elements = new ArrayList<>();

    SpaceObjectModel(SpaceObject observer) {
        SpaceObject.getTargets(observer).forEach(target -> elements.add(new SpaceObjectElement(target, this)));
    }

    List<SpaceObjectElement> getSelected() {
        ArrayList<SpaceObjectElement> selected = new ArrayList<>();
        for (SpaceObjectElement element : elements)
            if (element.isSelected())
                selected.add(element);
        return selected;
    }

    int indexOf(SpaceObject object) {
        for (int i = 0; i < elements.size(); i++) {
            SpaceObjectElement element = elements.get(i);
            if (element.isTarget(object))
                return i;
        }
        return -1;
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
