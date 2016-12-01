package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;

public class SWEKTreeModelSupplier extends SWEKTreeModelElement {

    /** The SWEK supplier for this SWEK tree supplier */
    private final SWEKSupplier swekSupplier;

    /**
     * Creates a SWEK tree supplier for the given SWEkSupplier.
     * 
     * @param swekSupplier
     *            The SWEK supplier for this SWEK tree supplier
     */
    public SWEKTreeModelSupplier(SWEKSupplier _swekSupplier) {
        super(false);
        swekSupplier = _swekSupplier;
    }

    /**
     * Gets the SWEK supplier.
     * 
     * @return The SWEK supplier
     */
    public SWEKSupplier getSwekSupplier() {
        return swekSupplier;
    }

}
