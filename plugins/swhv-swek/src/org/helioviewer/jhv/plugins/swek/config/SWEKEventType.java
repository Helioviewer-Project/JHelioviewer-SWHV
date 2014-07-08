/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Create a SWEKEvenType with an empty name, suppliers list, parameter list, not standard selected and grouped on nothing.
     */
    public SWEKEventType() {
        super();
        this.eventName = "";
        this.suppliers = new ArrayList<SWEKSupplier>();
        this.parameterList = new ArrayList<SWEKParameter>();
        this.requestIntervalExtension = 0L;
        this.standardSelected = false;
        this.groupOn = null;
    }

    /**
     * Creates an event type for the given event name, suppliers list, parameter list, request interval extension, standard selected indication and group on parameter.
     *
     * @param eventName                     The name of the event
     * @param suppliers                     The list of suppliers for this event type
     * @param parameterList                 The list of parameters for this event
     * @param requestIntervalExtension      The extension of the requested interval
     * @param standardSelected              Is the event type standard selected
     * @param groupOn                       On what are corresponding events grouped
     */
    public SWEKEventType(String eventName, List<SWEKSupplier> suppliers, List<SWEKParameter> parameterList, Long requestIntervalExtension, boolean standardSelected, SWEKParameter groupOn) {
        super();
        this.eventName = eventName;
        this.suppliers = suppliers;
        this.parameterList = parameterList;
        this.requestIntervalExtension = requestIntervalExtension;
        this.standardSelected = standardSelected;
        this.groupOn = groupOn;
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
     * @param eventName the eventName to set
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
     * @param suppliers the suppliers to set
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
     * @param parameterList the parameterList to set
     */
    public void setParameterList(List<SWEKParameter> parameterList) {
        this.parameterList = parameterList;
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
     * @param requestIntervalExtension the requestIntervalExtension to set
     */
    public void setRequestIntervalExtension(Long requestIntervalExtension) {
        this.requestIntervalExtension = requestIntervalExtension;
    }

    /**
     * Is this event type standard selected.
     *
     * @return the standardSelected     True if the event type is standard selected, false if not
     */
    public boolean isStandardSelected() {
        return standardSelected;
    }

    /**
     * Sets the event type standard selected if true, not standard selected if false.
     *
     * @param standardSelected  True if the event type is standart selected, false if not
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
     * @param groupOn the groupOn to set
     */
    public void setGroupOn(SWEKParameter groupOn) {
        this.groupOn = groupOn;
    }
}
