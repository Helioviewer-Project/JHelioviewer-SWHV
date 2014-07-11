package org.helioviewer.jhv.plugins.swek.model;

import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.config.SWEKSupplier;

/**
 * The SWEK tree model representation of the SWEK event type.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKTreeModelEventType extends AbstractSWEKTreeModelElement {
    /** The swekEventType for this treemodel event type */
    private final SWEKEventType swekEventType;

    /** List with SWEKSuppliers for this event type */
    private final List<SWEKTreeModelSupplier> swekTreeSuppliers;

    /**
     * Creates a SWEK tree model event type for the given SWEK event type.
     * 
     * @param swekEventType
     *            The event type for which the SWEK tree model event type is
     *            created
     */
    public SWEKTreeModelEventType(SWEKEventType swekEventType) {
        super(false);
        this.swekEventType = swekEventType;
        this.swekTreeSuppliers = new ArrayList<SWEKTreeModelSupplier>();
        fillSWEKTreeSuppliers();
    }

    /**
     * Gets the event type of this SWEK tree model event type.
     * 
     * @return the SWEK event type
     */
    public SWEKEventType getSwekEventType() {
        return this.swekEventType;
    }

    /**
     * Gets the list of SWEK tree model suppliers for this SWEK tree model event
     * type.
     * 
     * @return a list of SWEK tree model suppliers
     */
    public List<SWEKTreeModelSupplier> getSwekTreeSuppliers() {
        return this.swekTreeSuppliers;
    }

    /**
     * Fills the list of SWEK tree model suppliers for this swek event type.
     */
    private void fillSWEKTreeSuppliers() {
        for (SWEKSupplier swekSupplier : this.swekEventType.getSuppliers()) {
            this.swekTreeSuppliers.add(new SWEKTreeModelSupplier(swekSupplier));
        }
    }

}
