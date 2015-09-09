package org.helioviewer.jhv.plugins.swek.sources.comesep;

import java.awt.Color;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.datatype.event.AbstractJHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelationship;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

/**
 * Represents a JHVevent coming from the Comsep source.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class ComesepEvent extends AbstractJHVEvent {

    /** the start date of the event */
    private Date startDate;

    /** the end date of the event */
    private Date endDate;

    /** the icon */
    private final ImageIcon icon;

    /** the event name */
    private final String eventName;

    /** the event display name */
    private final String eventDisplayName;

    /** the event short description */
    private final String description;

    /** all the parameters */
    private Map<String, JHVEventParameter> allParameters;

    /** all the visible parameters */
    private Map<String, JHVEventParameter> allVisibleParameters;

    /** all the visible not null parameters */
    private Map<String, JHVEventParameter> allVisibleNotNullParameters;

    /** all the visible null parameters */
    private Map<String, JHVEventParameter> allVisibleNullParameters;

    /** all the non visible parameters */
    private Map<String, JHVEventParameter> allNonVisibleParameters;

    /** all the non visible not null parameters */
    private Map<String, JHVEventParameter> allNonVisibleNotNullParameters;

    /** all the non visible null parameters */
    private Map<String, JHVEventParameter> allNonVisibleNullParameters;

    /** The event type */
    private final JHVEventType eventType;

    /** The unique identifier */
    private String uniqueID;

    /** List with positioning information for this event */
    private HashMap<JHVCoordinateSystem, JHVPositionInformation> positionInformation;

    /** The color in which the event should be drawn */
    private final Color color;

    /** The event relation ships of the event */
    private final JHVEventRelationship eventRelationShip;

    /**
     * Default constructor
     */
    public ComesepEvent() {
        super();
        initLists();
        eventName = "";
        eventDisplayName = "";
        description = "";
        icon = null;
        eventType = null;
        color = Color.black;
        eventRelationShip = new JHVEventRelationship();
    }

    /**
     *
     * @param eventName
     * @param eventDisplayName
     * @param description
     * @param comesepEventType
     * @param eventIcon
     * @param color
     */
    public ComesepEvent(String eventName, String eventDisplayName, String description, ComesepEventType comesepEventType, ImageIcon eventIcon, Color color) {
        initLists();
        this.eventName = eventName;
        this.eventDisplayName = eventDisplayName;
        this.description = description;
        eventType = comesepEventType;
        icon = eventIcon;
        this.color = color;
        eventRelationShip = new JHVEventRelationship();
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getName() {
        return eventName;
    }

    @Override
    public String getDisplayName() {
        return eventDisplayName;
    }

    @Override
    public ImageIcon getIcon() {
        return icon;
    }

    @Override
    public String getShortDescription() {
        return description;
    }

    @Override
    public Map<String, JHVEventParameter> getAllEventParameters() {
        return allParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getVisibleEventParameters() {
        return allVisibleParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getVisibleNotNullEventParameters() {
        return allVisibleNotNullParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getVisibleNullEventParameters() {
        return allVisibleNullParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getNonVisibleEventParameters() {
        return allNonVisibleParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getNonVisibleNotNullEventParameters() {
        return allNonVisibleNotNullParameters;
    }

    @Override
    public Map<String, JHVEventParameter> getNonVisibleNullEventParameters() {
        return allNonVisibleNullParameters;
    }

    @Override
    public JHVEventType getJHVEventType() {
        return eventType;
    }

    @Override
    public String getUniqueID() {
        return uniqueID;
    }

    @Override
    public HashMap<JHVCoordinateSystem, JHVPositionInformation> getPositioningInformation() {
        return positionInformation;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public JHVEventRelationship getEventRelationShip() {
        return eventRelationShip;
    }

    @Override
    public void merge(JHVEvent event) {
        eventRelationShip.merge(event.getEventRelationShip());
    }

    /**
     * Adds a parameter to the event.
     *
     * @param parameter
     *            the parameter to add
     * @param visible
     *            is the parameter visible
     * @param configured
     *            was the event in the configuration file
     */
    public void addParameter(JHVEventParameter parameter, boolean visible, boolean configured) {
        allParameters.put(parameter.getParameterName(), parameter);
        if (configured) {
            if (visible) {
                allVisibleParameters.put(parameter.getParameterName(), parameter);
                if (parameter.getParameterValue() == null) {
                    allVisibleNullParameters.put(parameter.getParameterName(), parameter);
                } else {
                    allVisibleNotNullParameters.put(parameter.getParameterName(), parameter);
                }
            } else {
                allNonVisibleParameters.put(parameter.getParameterName(), parameter);
                if (parameter.getParameterValue() == null) {
                    allNonVisibleNullParameters.put(parameter.getParameterName(), parameter);
                } else {
                    allNonVisibleNotNullParameters.put(parameter.getParameterName(), parameter);
                }
            }
        }
    }

    /**
     * Sets the start date of the ComesepEvent.
     *
     * @param startDate
     *            the start date
     */
    public void setStartTime(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Sets the end date of the ComesepEvent.
     *
     * @param endDate
     *            the end date
     */
    public void setEndTime(Date endDate) {
        this.endDate = endDate;

    }

    /**
     * Sets the unique ID for the HekEvent.
     *
     * @param uniqueID
     */
    public void setUniqueID(String uniqueID) {
        this.uniqueID = uniqueID;
    }

    /**
     * Adds position information to the HEKEvent.
     *
     * @param positionInformation
     *            the position information to add
     */
    public void addJHVPositionInformation(JHVCoordinateSystem coorSys, JHVPositionInformation positionInformation) {
        this.positionInformation.put(coorSys, positionInformation);
    }

    /**
     * Initialize all the lists.
     */
    private void initLists() {
        allParameters = new HashMap<String, JHVEventParameter>();
        allVisibleParameters = new HashMap<String, JHVEventParameter>();
        allVisibleNotNullParameters = new HashMap<String, JHVEventParameter>();
        allVisibleNullParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleNotNullParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleNullParameters = new HashMap<String, JHVEventParameter>();
        positionInformation = new HashMap<JHVCoordinateSystem, JHVPositionInformation>();
    }
}
