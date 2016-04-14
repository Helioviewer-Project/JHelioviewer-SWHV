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
    private final String db;

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
    public SWEKSupplier(String supplierName, String supplierDisplayName, SWEKSource source, String db) {
        String key = supplierName + source.getSourceName() + db;
        this.supplierName = supplierName;
        this.supplierDisplayName = supplierDisplayName;
        this.source = source;
        this.db = db;
        suppliers.put(key, this);
    }

    public static SWEKSupplier getSupplier(String supplierNameKey) {
        return suppliers.get(supplierNameKey);
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

    public String getDatabaseName() {
        return db;
    }

    /**
     * Gets the source on which the supplier supplies its events.
     *
     * @return the source
     */
    public SWEKSource getSource() {
        return source;
    }

    public String getSupplierKey() {
        return supplierName + source.getSourceName() + db;
    }

}
