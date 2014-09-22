package org.helioviewer.jhv.plugins.swek.model;

import javax.swing.ImageIcon;

/**
 * Abstract class combining general functionality of the SWEK tree elements
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public abstract class AbstractSWEKTreeModelElement {
    /** Is the check box selected */
    private boolean checkboxSelected;

    /** The icon */
    private ImageIcon icon;

    /**
     * Default constructor
     */
    public AbstractSWEKTreeModelElement() {
        checkboxSelected = false;
        icon = null;
    }

    /**
     * Creates an Abstract SWEK Tree element with the checkbox selected or not.
     * 
     * @param isCheckBoxSelected
     *            Is the checkbox selected.
     */
    public AbstractSWEKTreeModelElement(boolean isCheckBoxSelected) {
        checkboxSelected = isCheckBoxSelected;
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
    public AbstractSWEKTreeModelElement(boolean isCheckBoxSelected, ImageIcon icon) {
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
