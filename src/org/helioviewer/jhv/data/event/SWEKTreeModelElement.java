package org.helioviewer.jhv.data.event;

import javax.swing.ImageIcon;

public class SWEKTreeModelElement {

    private boolean selected;
    private ImageIcon icon;
    private String name;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean _selected) {
        selected = _selected;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    protected void setIcon(ImageIcon _icon) {
        icon = _icon;
    }

    public String getDisplayName() {
        return name;
    }

    protected void setDisplayName(String _name) {
        name = _name;
    }

}
