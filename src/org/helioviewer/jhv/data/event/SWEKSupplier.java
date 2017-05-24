package org.helioviewer.jhv.data.event;

import java.util.HashMap;

import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.json.JSONObject;

public class SWEKSupplier extends SWEKTreeModelElement {

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
     * @param _supplierName
     *            The name of the supplier
     * @param _supplierDisplayName
     *            The display name of the supplier
     * @param _source
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

    public void serialize(JSONObject suppliers) {
    }
    
    public void deserialize(JSONObject suppliers, EventTypePanelModel eventPanelModel, SWEKEventType swekEventType) {
    }

}
