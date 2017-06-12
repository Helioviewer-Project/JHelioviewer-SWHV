package org.helioviewer.jhv.camera.object;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.SpaceObject;
import org.helioviewer.jhv.camera.UpdateViewpoint;

@SuppressWarnings("serial")
public class SpaceObjectModel extends AbstractTableModel {

    private final ArrayList<SpaceObjectElement> elementList = new ArrayList<>();

    public SpaceObjectModel() {
        for (SpaceObject object : SpaceObject.getObjectList())
            elementList.add(new SpaceObjectElement(object));
    }

    public void deselectAll(UpdateViewpoint _uv) {
        for (SpaceObjectElement element : elementList) {
            if (element.isSelected())
                element.deselect(_uv);
        }
    }

    public void loadSelected(UpdateViewpoint _uv, String _frame, long _startTime, long _endTime) {
        for (SpaceObjectElement element : elementList) {
            if (element.isSelected())
                element.select(_uv, _frame, _startTime, _endTime); // force load
        }
    }

    public SpaceObjectElement selectionElement(SpaceObject object) {
        for (SpaceObjectElement element : elementList) {
            if (element.getObject() == object)
                return element;
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return elementList.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(int row, int column) {
        return elementList.get(row);
    }

}
