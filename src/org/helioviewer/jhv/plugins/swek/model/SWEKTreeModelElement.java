package org.helioviewer.jhv.plugins.swek.model;

import javax.swing.ImageIcon;

public class SWEKTreeModelElement {

    private boolean checkboxSelected;
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

    public ImageIcon getIcon() {
        return icon;
    }

}
