package org.helioviewer.jhv.camera.object;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.camera.UpdateViewpoint;

@SuppressWarnings("serial")
public class SpaceObjectModel extends AbstractTableModel {

    private final ArrayList<SpaceObjectElement> elements = new ArrayList<>();

    public SpaceObjectModel() {
        for (SpaceObject object : SpaceObject.getObjectList())
            elements.add(new SpaceObjectElement(object, this));
    }

    public void deselectAll(UpdateViewpoint uv) {
        for (SpaceObjectElement element : elements) {
            if (element.isSelected())
                element.deselect(uv);
        }
    }

    public void loadSelected(UpdateViewpoint uv, String frame, long startTime, long endTime) {
        for (SpaceObjectElement element : elements) {
            if (element.isSelected())
                element.select(uv, frame, startTime, endTime); // force load
        }
    }

    public SpaceObjectElement selectionElement(SpaceObject object) {
        for (SpaceObjectElement element : elements) {
            if (element.getObject() == object)
                return element;
        }
        return null;
    }

    public void refresh(SpaceObjectElement element) {
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
