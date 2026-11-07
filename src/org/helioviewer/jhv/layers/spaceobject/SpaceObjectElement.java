package org.helioviewer.jhv.layers.spaceobject;

import org.helioviewer.jhv.astronomy.SpaceObject;

public final class SpaceObjectElement {

    private final SpaceObject target;

    private boolean selected;
    private String status;

    public SpaceObjectElement(SpaceObject _target) {
        target = _target;
    }

    public SpaceObject getTarget() {
        return target;
    }

    void select() {
        selected = true;
        status = null;
    }

    void deselect() {
        selected = false;
        status = null;
    }

    void setStatus(String _status) {
        status = _status;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return target.toString();
    }

}
