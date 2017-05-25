package org.helioviewer.jhv.data.event;

import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.json.JSONObject;

public class SWEKGroup extends SWEKTreeModelElement {

    private static List<SWEKRelatedEvents> swekrelEvents;
    private final List<SWEKParameter> parameterList;

    private final boolean containsParameterFilter;

    private List<SWEKSupplier> suppliers;
    private HashMap<String, String> databaseFields;

    public SWEKGroup(String _eventName, List<SWEKParameter> _parameterList, ImageIcon _eventIcon) {
        setDisplayName(_eventName);
        parameterList = _parameterList;
        setIcon(_eventIcon);
        containsParameterFilter = checkFilters(parameterList);
    }

    public HashMap<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEKParameter p : parameterList) {
            SWEKParameterFilter pf = p.getParameterFilter();
            if (pf != null) {
                fields.put(p.getParameterName().intern(), pf.getDbType());
            }
        }
        for (SWEKRelatedEvents re : swekrelEvents) {
            if (re.getGroup() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.parameterFrom;
                    fields.put(p.getParameterName().intern(), swon.dbType);
                }
            }
            if (re.getRelatedWith() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.parameterWith;
                    fields.put(p.getParameterName().intern(), swon.dbType);
                }
            }
        }
        databaseFields = fields;
    }

    public static void setSwekRelatedEvents(List<SWEKRelatedEvents> _relatedEvents) {
        swekrelEvents = _relatedEvents;
    }

    public static List<SWEKRelatedEvents> getSWEKRelatedEvents() {
        return swekrelEvents;
    }

    public List<SWEKSupplier> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<SWEKSupplier> _suppliers) {
        suppliers = _suppliers;
    }

    /**
     * Get the list of event type specific parameters
     *
     * @return the parameterList
     */
    public List<SWEKParameter> getParameterList() {
        return parameterList;
    }

    /**
     * Gets a parameter from the event type.
     *
     * @param name
     *            the name of the parameter defined in the swek source
     * @return the parameter if present in the event type, null if the parameter
     *         was not found.
     */
    public SWEKParameter getParameter(String name) {
        for (SWEKParameter parameter : parameterList) {
            if (parameter.getParameterName().equalsIgnoreCase(name)) {
                return parameter;
            }
        }
        return null;
    }

    /**
     * Checks if the event type contains parameter filter.
     *
     * @return
     */
    public boolean containsFilter() {
        return containsParameterFilter;
    }

    /**
     * Checks if the parameter list contains filters.
     *
     * @param parameters
     *            the list of parameter for this event type
     * @return true if there are filters in this event type, false if not
     */
    private static boolean checkFilters(List<SWEKParameter> parameters) {
        for (SWEKParameter parameter : parameters) {
            if (parameter.getParameterFilter() != null) {
                return true;
            }
        }
        return false;
    }

    public void serialize(JSONObject swekObject) {
    }

    public void deserialize(JSONObject swekObject) {
    }

}
