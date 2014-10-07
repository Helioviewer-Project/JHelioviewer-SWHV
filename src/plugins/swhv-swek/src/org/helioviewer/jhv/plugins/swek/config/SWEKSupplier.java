/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

/**
 * Defines an event type supplier.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKSupplier {
    /** Name of the supplier */
    private String supplierName;

    /** The source from where is supplied */
    private SWEKSource source;

    /** The display name of the supplier */
    private String supplierDisplayName;

    /**
     * Creates a SWEK supplier with an empty name and null source.
     */
    public SWEKSupplier() {
        super();
        supplierName = "";
        supplierDisplayName = "";
        source = null;
    }

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
        super();
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
     * Sets the supplier name.
     * 
     * @param supplierName
     *            the supplierName to set
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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
     * Sets the supplier display name.
     * 
     * @param supplierDisplayName
     *            the supplier display name
     */
    public void setSupplierDisplayName(String supplierDisplayName) {
        this.supplierDisplayName = supplierDisplayName;
    }

    /**
     * Gets the source on which the supplier supplies its events.
     * 
     * @return the source
     */
    public SWEKSource getSource() {
        return source;
    }

    /**
     * Sets the source on which the supplier supplies its events.
     * 
     * @param source
     *            the source to set
     */
    public void setSource(SWEKSource source) {
        this.source = source;
    }

}
