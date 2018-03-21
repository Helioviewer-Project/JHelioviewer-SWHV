package org.helioviewer.jhv.data.event;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.gui.filter.FilterDialog;

public abstract class SWEKTreeModelElement {

    private boolean selected;
    protected ImageIcon icon;
    protected String name;
    protected FilterDialog filterDialog;

    public boolean isSelected() {
        return selected;
    }

    void setSelected(boolean _selected) {
        selected = _selected;
    }

    @Nullable
    public ImageIcon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public FilterDialog getFilterDialog() {
        return filterDialog;
    }

    public abstract void activate(boolean _activate);

}
