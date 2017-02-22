package org.helioviewer.jhv.plugins.swek.model;

import javax.swing.ImageIcon;

import org.jetbrains.annotations.Nullable;

public class SWEKTreeModelElement {

    private boolean checkboxSelected;
    @Nullable
    private final ImageIcon icon;

    SWEKTreeModelElement(boolean isCheckBoxSelected) {
        checkboxSelected = isCheckBoxSelected;
        icon = null;
    }

    SWEKTreeModelElement(boolean isCheckBoxSelected, ImageIcon _icon) {
        checkboxSelected = isCheckBoxSelected;
        icon = _icon;
    }

    public boolean isCheckboxSelected() {
        return checkboxSelected;
    }

    public void setCheckboxSelected(boolean _checkboxSelected) {
        checkboxSelected = _checkboxSelected;
    }

    @Nullable
    public ImageIcon getIcon() {
        return icon;
    }

}
