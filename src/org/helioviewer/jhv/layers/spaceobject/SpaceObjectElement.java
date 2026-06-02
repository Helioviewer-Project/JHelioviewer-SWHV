package org.helioviewer.jhv.layers.spaceobject;

public final class SpaceObjectElement {

    private final String name;

    private boolean selected;
    private String status;

    public SpaceObjectElement(String _name) {
        name = _name;
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
        return name;
    }

}
