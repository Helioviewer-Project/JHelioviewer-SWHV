package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

public class SWEKEventType {
    private static List<SWEKRelatedEvents> swekrelEvents;
    private final String eventName;
    private final List<SWEKSupplier> suppliers;
    private final List<SWEKParameter> parameterList;
    private final Long requestIntervalExtension;
    private final boolean standardSelected;
    private final ImageIcon eventIcon;
    private final Color color;

    private final boolean containsParameterFilter;
    private HashMap<String, String> databaseFields;
    private static final HashMap<String, SWEKEventType> swekEventTypes = new HashMap<String, SWEKEventType>();

    public SWEKEventType(String eventName, List<SWEKSupplier> suppliers, List<SWEKParameter> parameterList, Long requestIntervalExtension, boolean standardSelected, ImageIcon eventIcon, Color color) {
        this.eventName = eventName.intern();
        this.suppliers = suppliers;
        this.parameterList = parameterList;
        this.requestIntervalExtension = requestIntervalExtension;
        this.standardSelected = standardSelected;
        this.eventIcon = eventIcon;
        this.color = color;
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
        HashMap<String, String> fields = new HashMap<String, String>();
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
     * Gets the time extension of the requested time interval.
     *
     * @return the requestIntervalExtension
     */
    public Long getRequestIntervalExtension() {
        return requestIntervalExtension;
    }

    /**
     * Is this event type standard selected.
     *
     * @return the standardSelected True if the event type is standard selected,
     *         false if not
     */
    public boolean isStandardSelected() {
        return standardSelected;
    }

    /**
     * Contains this source the following parameter.
     *
     * @param name
     *            the name of the parameter
     * @return true if the parameter is configured for this source, false if the
     *         parameter is not configured for this source
     */
    public boolean containsParameter(String name) {
        for (SWEKParameter parameter : parameterList) {
            if (parameter.getParameterName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
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
     * Gets the color of the event type.
     *
     * @return the color of the event type
     */
    public Color getColor() {
        return color;
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
