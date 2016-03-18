package org.helioviewer.jhv.data.datatype.event;

import java.util.HashMap;

/**
 * Defines an event type supplier.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKSupplier {

    /** Name of the supplier */
    private final String supplierName;

    /** The source from where is supplied */
    private final SWEKSource source;

    /** The display name of the supplier */
    private final String supplierDisplayName;
    private static HashMap<String, SWEKSupplier> suppliers = new HashMap<String, SWEKSupplier>();

    /**
     * Creates a SWEK supplier with an supplier name and a source.
     *
     * @param supplierName
     *            The name of the supplier
     * @param supplierDisplayName
     *            The display name of the supplier
     * @param source
     *            The source on which the supplier supplies its events
     */
    private SWEKSupplier(String supplierName, String supplierDisplayName, SWEKSource source) {

        this.supplierName = supplierName;
        this.supplierDisplayName = supplierDisplayName;
        this.source = source;
    }

    public static SWEKSupplier getSupplier(String supplierName, String supplierDisplayName, SWEKSource source) {
        SWEKSupplier supp = suppliers.get(supplierName + source.getSourceName());
        if (supp == null) {
            return new SWEKSupplier(supplierName, supplierDisplayName, source);
        }
        return supp;
    }

    /**
     * Gets the supplier name.
     *
     * @return the supplierName
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * Gets the display name of the supplier
     *
     * @return the display name of the supplier.
     */
    public String getSupplierDisplayName() {
        return supplierDisplayName;
    }

    /**
     * Gets the source on which the supplier supplies its events.
     *
     * @return the source
     */
    public SWEKSource getSource() {
        return source;
    }

}
