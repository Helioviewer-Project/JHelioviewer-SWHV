/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * Describes a SWEK event type.
 * 
 * @author Bram Bourgoignie (Bram.bourgoignie@oma.be)
 * 
 */
public class SWEKEventType {
    /** The name of the event */
    private String eventName;

    /** List of suppliers of the event */
    private List<SWEKSupplier> suppliers;

    /** List of event specific parameters */
    private List<SWEKParameter> parameterList;

    /** Extension of the request interval for this event type */
    private Long requestIntervalExtension;

    /** Is the event type standard selected */
    private boolean standardSelected;

    /** On what should events be grouped on */
    private SWEKParameter groupOn;

    /** The coordinate system */
    private String coordinateSystem;

    /** The spatial region over which the event can be found */
    private SWEKSpatialRegion spatialRegion;

    /** The icon corresponding with the event type */
    private ImageIcon eventIcon;

    /** The color used for this event type */
    private Color color;

    private boolean containsParameterFilter;

    /**
     * Create a SWEKEvenType with an empty name, suppliers list, parameter list,
     * not standard selected, grouped on nothing, no coordinate system, no
     * spatial region, event icon null and color black.
     */
    public SWEKEventType() {
        super();
        eventName = "";
        suppliers = new ArrayList<SWEKSupplier>();
        parameterList = new ArrayList<SWEKParameter>();
        requestIntervalExtension = 0L;
        standardSelected = false;
        groupOn = null;
        coordinateSystem = "";
        spatialRegion = new SWEKSpatialRegion();
        eventIcon = null;
        color = Color.BLACK;
        containsParameterFilter = false;
    }

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
     */
    public SWEKEventType(String eventName, List<SWEKSupplier> suppliers, List<SWEKParameter> parameterList, Long requestIntervalExtension,
            boolean standardSelected, SWEKParameter groupOn, String coordinateSystem, ImageIcon eventIcon, Color color) {
        super();
        this.eventName = eventName;
        this.suppliers = suppliers;
        this.parameterList = parameterList;
        this.requestIntervalExtension = requestIntervalExtension;
        this.standardSelected = standardSelected;
        this.groupOn = groupOn;
        this.coordinateSystem = coordinateSystem;
        this.eventIcon = eventIcon;
        this.color = color;
        containsParameterFilter = checkFilters(parameterList);
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
     * Sets the name of the event type.
     * 
     * @param eventName
     *            the eventName to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
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
     * Sets the list of suppliers.
     * 
     * @param suppliers
     *            the suppliers to set
     */
    public void setSuppliers(List<SWEKSupplier> suppliers) {
        this.suppliers = suppliers;
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
     * Sets the list of event type specific parameters.
     * 
     * @param parameterList
     *            the parameterList to set
     */
    public void setParameterList(List<SWEKParameter> parameterList) {
        this.parameterList = parameterList;
        containsParameterFilter = checkFilters(parameterList);
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
     * Sets the extension of the requested time interval.
     * 
     * @param requestIntervalExtension
     *            the requestIntervalExtension to set
     */
    public void setRequestIntervalExtension(Long requestIntervalExtension) {
        this.requestIntervalExtension = requestIntervalExtension;
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
     * Sets the event type standard selected if true, not standard selected if
     * false.
     * 
     * @param standardSelected
     *            True if the event type is standart selected, false if not
     */
    public void setStandardSelected(boolean standardSelected) {
        this.standardSelected = standardSelected;
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
     * Sets the parameter on which corresponding events should be grouped.
     * 
     * @param groupOn
     *            the groupOn to set
     */
    public void setGroupOn(SWEKParameter groupOn) {
        this.groupOn = groupOn;
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
     * Sets the coordinate system.
     * 
     * @param coordinateSystem
     *            the coordinate system
     */
    public void setCoordinateSystem(String coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
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
     * Sets the spatial region for this event.
     * 
     * @param spatialRegion
     *            The spatial region
     */
    public void setSpatialRegion(SWEKSpatialRegion spatialRegion) {
        this.spatialRegion = spatialRegion;
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
     * Sets the event icon.
     * 
     * @param eventIcon
     *            the icon of the event type
     */
    public void setEventIcon(ImageIcon eventIcon) {
        this.eventIcon = eventIcon;
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
     * Sets the color of the event type.
     * 
     * @param color
     *            the color of the event type
     */
    public void setColor(Color color) {
        this.color = color;
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
