package org.helioviewer.jhv.events.gui;

import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import org.helioviewer.jhv.events.gui.filter.FilterDialog;

public abstract class SWEKTreeModelElement {

    private boolean selected;
    protected String name;
    protected FilterDialog filterDialog;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean _selected) {
        selected = _selected;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public FilterDialog getFilterDialog() {
        return filterDialog;
    }

    public abstract void activate(boolean _activate);

    public abstract ImageIcon getIcon();

}
