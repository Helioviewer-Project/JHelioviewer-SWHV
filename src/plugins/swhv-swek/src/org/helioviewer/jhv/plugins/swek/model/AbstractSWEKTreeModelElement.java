package org.helioviewer.jhv.plugins.swek.model;

/**
 * Abstract class combining general functionality of the SWEK tree elements
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public abstract class AbstractSWEKTreeModelElement {
    /** Is the check box selected */
    private boolean checkboxSelected;

    /**
     * Default constructor
     */
    public AbstractSWEKTreeModelElement() {
        this.checkboxSelected = false;
    }

    /**
     * Creates a Abstract SWEK Tree element.
     * 
     * @param isCheckBoxSelected
     *            Is the checkbox selected.
     */
    public AbstractSWEKTreeModelElement(boolean isCheckBoxSelected) {
        this.checkboxSelected = isCheckBoxSelected;
    }

    /**
     * Is the checkbox selected.
     * 
     * @return true if the checkbox is selected, false if not.
     */
    public boolean isCheckboxSelected() {
        return this.checkboxSelected;
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
}
