package org.helioviewer.jhv.plugins.swek.model;

import javax.swing.ImageIcon;

// Abstract class combining general functionality of the SWEK tree elements
public abstract class AbstractSWEKTreeModelElement {

    private boolean checkboxSelected;

    private final ImageIcon icon;

    AbstractSWEKTreeModelElement() {
        checkboxSelected = false;
        icon = null;
    }

    /**
     * Creates an Abstract SWEK Tree element with the checkbox selected or not.
     * 
     * @param isCheckBoxSelected
     *            Is the checkbox selected
     */
    AbstractSWEKTreeModelElement(boolean isCheckBoxSelected) {
        checkboxSelected = isCheckBoxSelected;
        icon = null;
    }

    /**
     * Creates an Abstract SWEK tree element with a given icon.
     * 
     * @param icon
     *            the icon for this SWEKTreeModel element
     */
    public AbstractSWEKTreeModelElement(ImageIcon icon) {
        this.icon = icon;
        checkboxSelected = false;
    }

    /**
     * Creates an Abstract SWEK tree element with the checkbox selected or not
     * and a given icon.
     * 
     * @param isCheckBoxSelected
     *            is the checkbox selected
     * @param icon
     *            the icon for this SWEKTreeModel element
     */
    AbstractSWEKTreeModelElement(boolean isCheckBoxSelected, ImageIcon icon) {
        checkboxSelected = isCheckBoxSelected;
        this.icon = icon;
    }

    /**
     * Is the checkbox selected.
     * 
     * @return true if the checkbox is selected, false if not.
     */
    public boolean isCheckboxSelected() {
        return checkboxSelected;
    }

    /**
     * Gives the checkbox a new selection.
     * 
     * @param checkboxSelected
     *            true if the checkbox must be selected, false if not.
     */
    public void setCheckboxSelected(boolean checkboxSelected) {
        this.checkboxSelected = checkboxSelected;
    }

    /**
     * Gets the icon for this abstract SWEK tree event type.
     * 
     * @return the icon for this abstract SWEK tree event type
     */
    public ImageIcon getIcon() {
        return icon;
    }

}
