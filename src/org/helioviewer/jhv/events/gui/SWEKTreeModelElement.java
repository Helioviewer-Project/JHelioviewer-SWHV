package org.helioviewer.jhv.events.gui;

public abstract class SWEKTreeModelElement {

    private boolean selected;
    protected String name;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean _selected) {
        selected = _selected;
    }

    public String getName() {
        return name;
    }

    public abstract void activate(boolean _activate);

}
