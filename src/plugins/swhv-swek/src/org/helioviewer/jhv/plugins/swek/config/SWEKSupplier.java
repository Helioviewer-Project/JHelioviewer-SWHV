package org.helioviewer.jhv.plugins.swek.config;

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
    public SWEKSupplier(String supplierName, String supplierDisplayName, SWEKSource source) {
        this.supplierName = supplierName;
        this.supplierDisplayName = supplierDisplayName;
        this.source = source;
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
