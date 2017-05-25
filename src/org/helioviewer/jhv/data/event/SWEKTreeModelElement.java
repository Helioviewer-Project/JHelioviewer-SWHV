package org.helioviewer.jhv.data.event;

import javax.swing.ImageIcon;

public class SWEKTreeModelElement {

    private boolean selected;
    protected ImageIcon icon;
    protected String name;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean _selected) {
        selected = _selected;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

}
