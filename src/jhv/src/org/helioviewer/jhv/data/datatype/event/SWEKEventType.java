package org.helioviewer.jhv.data.datatype.event;

import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

public class SWEKEventType {
    private static List<SWEKRelatedEvents> swekrelEvents;
    private final String eventName;
    private final List<SWEKSupplier> suppliers;
    private final List<SWEKParameter> parameterList;
    private final ImageIcon eventIcon;

    private final boolean containsParameterFilter;
    private HashMap<String, String> databaseFields;
    private static final HashMap<String, SWEKEventType> swekEventTypes = new HashMap<>();

    public SWEKEventType(String eventName, List<SWEKSupplier> suppliers, List<SWEKParameter> parameterList, ImageIcon eventIcon) {
        this.eventName = eventName.intern();
        this.suppliers = suppliers;
        this.parameterList = parameterList;
        this.eventIcon = eventIcon;
        containsParameterFilter = checkFilters(parameterList);
        swekEventTypes.put(this.eventName, this);
    }

    public static SWEKEventType getEventType(String name) {
        return swekEventTypes.get(name);
    }

    public HashMap<String, String> getAllDatabaseFields() {
        if (databaseFields == null) {
            createAllDatabaseFields();
        }
        return databaseFields;
    }

    private void createAllDatabaseFields() {
        HashMap<String, String> fields = new HashMap<>();
        for (SWEKParameter p : getParameterList()) {
            SWEKParameterFilter pf = p.getParameterFilter();
            if (pf != null) {
                fields.put(p.getParameterName().intern(), pf.getDbType());
            }
        }
        for (SWEKRelatedEvents re : getSWEKRelatedEvents()) {
            if (re.getEvent() == this) {
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

    public List<SWEKRelatedEvents> getSWEKRelatedEvents() {
        return swekrelEvents;
    }

    /**
     * Gives the name of the event type.
     *
     * @return the eventName
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Gets the list of suppliers.
     *
     * @return the suppliers
     */
    public List<SWEKSupplier> getSuppliers() {
        return suppliers;
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
     * Gets the event icon.
     *
     * @return the icon of the event type
     */
    public ImageIcon getEventIcon() {
        return eventIcon;
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
    private boolean checkFilters(List<SWEKParameter> parameters) {
        for (SWEKParameter parameter : parameters) {
            if (parameter.getParameterFilter() != null) {
                return true;
            }
        }
        return false;
    }

}
