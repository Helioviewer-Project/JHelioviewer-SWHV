package org.helioviewer.jhv.data.datatype.event;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * Describes a SWEK event type.
 *
 * @author Bram Bourgoignie (Bram.bourgoignie@oma.be)
 *
 */
public class SWEKEventType {
    private static List<SWEKRelatedEvents> swekrelEvents;
    /** The name of the event */
    private final String eventName;

    /** List of suppliers of the event */
    private final List<SWEKSupplier> suppliers;

    /** List of event specific parameters */
    private final List<SWEKParameter> parameterList;

    /** Extension of the request interval for this event type */
    private final Long requestIntervalExtension;

    /** Is the event type standard selected */
    private final boolean standardSelected;

    /** On what should events be grouped on */
    private final SWEKParameter groupOn;

    /** The coordinate system */
    private final String coordinateSystem;

    /** The spatial region over which the event can be found */
    private final SWEKSpatialRegion spatialRegion;

    /** The icon corresponding with the event type */
    private final ImageIcon eventIcon;

    /** The color used for this event type */
    private final Color color;

    private final boolean containsParameterFilter;
    private HashMap<String, String> databaseFields;
    private static HashMap<String, SWEKEventType> swekEventTypes = new HashMap<String, SWEKEventType>();

    /**
     * Creates an event type for the given event name, suppliers list, parameter
     * list, request interval extension, standard selected indication, group on
     * parameter, coordinate system, icon and color.
     *
     * @param eventName
     *            The name of the event
     * @param suppliers
     *            The list of suppliers for this event type
     * @param parameterList
     *            The list of parameters for this event
     * @param requestIntervalExtension
     *            The extension of the requested interval
     * @param standardSelected
     *            Is the event type standard selected
     * @param groupOn
     *            On what are corresponding events grouped
     * @param coordinateSystem
     *            The coordinate system
     * @param eventIcon
     *            the icon of the event type
     * @param color
     *            the color of the event type
     * @param list
     */
    public SWEKEventType(String eventName, List<SWEKSupplier> suppliers, List<SWEKParameter> parameterList, Long requestIntervalExtension, boolean standardSelected, SWEKParameter groupOn, String coordinateSystem, ImageIcon eventIcon, Color color, SWEKSpatialRegion spatialRegion) {
        this.eventName = eventName.intern();
        this.suppliers = suppliers;
        this.parameterList = parameterList;
        this.requestIntervalExtension = requestIntervalExtension;
        this.standardSelected = standardSelected;
        this.groupOn = groupOn;
        this.coordinateSystem = coordinateSystem;
        this.eventIcon = eventIcon;
        this.color = color;
        containsParameterFilter = checkFilters(parameterList);
        this.spatialRegion = spatialRegion;
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
        for (SWEKRelatedEvents re : getSwekRelatedEvents()) {
            if (re.getEvent() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.getParameterFrom();
                    String dbtype = swon.getDatabaseType();
                    fields.put(p.getParameterName().intern(), dbtype);
                }
            }
            if (re.getRelatedWith() == this) {
                List<SWEKRelatedOn> relon = re.getRelatedOnList();

                for (SWEKRelatedOn swon : relon) {
                    SWEKParameter p = swon.getParameterWith();
                    String dbtype = swon.getDatabaseType();
                    fields.put(p.getParameterName().intern(), dbtype);
                }
            }
        }
        databaseFields = fields;
    }

    public static void setSwekRelatedEvents(List<SWEKRelatedEvents> _relatedEvents) {
        swekrelEvents = _relatedEvents;
    }

    public List<SWEKRelatedEvents> getSwekRelatedEvents() {
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
     * Gives the parameter on which corresponding events should be grouped.
     *
     * @return the groupOn
     */
    public SWEKParameter getGroupOn() {
        return groupOn;
    }

    /**
     * Gets the coordinate system.
     *
     * @return the coordinate system
     */
    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Gets the spatial region for this event type.
     *
     * @return The spatial region
     */
    public SWEKSpatialRegion getSpatialRegion() {
        return spatialRegion;
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
            if (parameter.getParameterName().toLowerCase().equals(name.toLowerCase())) {
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
            if (parameter.getParameterName().toLowerCase().equals(name.toLowerCase())) {
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
