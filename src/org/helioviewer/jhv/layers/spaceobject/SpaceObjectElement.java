package org.helioviewer.jhv.layers.spaceobject;

public final class SpaceObjectElement {

    private final String name;
    private SpaceObjectModel model;

    private boolean selected;
    private String status;

    public SpaceObjectElement(String _name) {
        name = _name;
    }

    void setModel(SpaceObjectModel _model) {
        model = _model;
    }

    void select() {
        selected = true;
        status = null;
        model.refresh(this);
    }

    void deselect() {
        selected = false;
        status = null;
        model.refresh(this);
    }

    public void setStatus(String _status) {
        status = _status;
        if (model != null)
            model.refresh(this);
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
