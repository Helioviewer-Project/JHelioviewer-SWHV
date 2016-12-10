package org.helioviewer.jhv.data.datatype.event;

import java.util.HashMap;

// Defines an event type supplier.
public class SWEKSupplier {

    /** Name of the supplier */
    private final String supplierName;
    private final String db;

    /** The source from where is supplied */
    private final SWEKSource source;

    /** The display name of the supplier */
    private final String supplierDisplayName;
    private static final HashMap<String, SWEKSupplier> suppliers = new HashMap<>();

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
    public SWEKSupplier(String _supplierName, String _supplierDisplayName, SWEKSource _source, String _db) {
        supplierName = _supplierName;
        supplierDisplayName = _supplierDisplayName;
        source = _source;
        db = _db;

        String key = supplierName + source.getSourceName() + db;
        suppliers.put(key, this);
    }

    public static SWEKSupplier getSupplier(String supplierNameKey) {
        return suppliers.get(supplierNameKey);
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getSupplierDisplayName() {
        return supplierDisplayName;
    }

    public String getDatabaseName() {
        return db;
    }

    public SWEKSource getSource() {
        return source;
    }

    public String getSupplierKey() {
        return supplierName + source.getSourceName() + db;
    }

}
