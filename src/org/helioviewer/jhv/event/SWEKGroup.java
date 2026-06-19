package org.helioviewer.jhv.event;

import java.util.HashMap;
import java.util.Map;

public final class SWEKGroup {

    private final String name;
    private final String iconKey;

    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _name, String _iconKey) {
        name = _name.intern();
        iconKey = _iconKey;
    }

    public Map<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEK.RelatedEvents re : SWEKCatalog.getRelatedEvents()) {
            if (re.group() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterFrom().name().intern(), swon.dbType()));
            }
            if (re.relatedWith() == this) {
                re.relatedOnList().forEach(swon -> fields.put(swon.parameterWith().name().intern(), swon.dbType()));
            }
        }
        databaseFields = fields;
    }

    public String getName() {
        return name;
    }

    public String getIconKey() {
        return iconKey;
    }

}
