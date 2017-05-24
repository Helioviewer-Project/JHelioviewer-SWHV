package org.helioviewer.jhv.data.event;

import javax.swing.ImageIcon;

public class SWEKTreeModelElement {

    private boolean checkboxSelected;
    private ImageIcon icon;

    public boolean isCheckboxSelected() {
        return checkboxSelected;
    }

    public void setCheckboxSelected(boolean _checkboxSelected) {
        checkboxSelected = _checkboxSelected;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    protected void setIcon(ImageIcon _icon) {
        icon = _icon;
    }

}
