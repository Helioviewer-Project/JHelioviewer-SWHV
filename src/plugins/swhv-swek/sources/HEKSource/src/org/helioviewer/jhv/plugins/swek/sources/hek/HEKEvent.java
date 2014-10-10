package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.data.datatype.event.JHVCoordinateSystem;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;

/**
 * Represents a JHVevent coming from the HEK source.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class HEKEvent implements JHVEvent {

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
    private List<JHVEventParameter> allParameters;

    /** all the visible parameters */
    private List<JHVEventParameter> allVisibleParameters;

    /** all the visible not null parameters */
    private List<JHVEventParameter> allVisibleNotNullParameters;

    /** all the visible null parameters */
    private List<JHVEventParameter> allVisibleNullParameters;

    /** all the non visible parameters */
    private List<JHVEventParameter> allNonVisibleParameters;

    /** all the non visible not null parameters */
    private List<JHVEventParameter> allNonVisibleNotNullParameters;

    /** all the non visible null parameters */
    private List<JHVEventParameter> allNonVisibleNullParameters;

    /** The event type */
    private final JHVEventType eventType;

    /** The unique identifier */
    private String uniqueID;

    /** List with positioning information for this event */
    private HashMap<JHVCoordinateSystem, JHVPositionInformation> positionInformation;

    /** The color in which the event should be drawn */
    private final Color color;

    /**
     * Default constructor
     */
    public HEKEvent() {
        initLists();
        eventName = "";
        eventDisplayName = "";
        description = "";
        icon = null;
        eventType = null;
        color = Color.black;
    }

    /**
     * Creates a HEK event with an event name, event display name, short
     * description, an event type and color.
     *
     *
     * @param eventName
     *            the event name
     * @param eventDisplayName
     *            the event display name
     * @param description
     *            the short description
     * @param eventType
     *            the event type
     * @param color
     *            the color
     */
    public HEKEvent(String eventName, String eventDisplayName, String description, JHVEventType eventType, Color color) {
        initLists();
        this.eventName = eventName;
        this.eventDisplayName = eventDisplayName;
        this.description = description;
        icon = null;
        this.eventType = eventType;
        this.color = color;
    }

    /**
     * Creates a HEK event with an event name, event display name, short
     * description, event type, an icon and color.
     *
     * @param eventName
     *            the event name
     * @param eventDisplayName
     *            the display name
     * @param description
     *            the description
     * @param icon
     *            the icon
     * @param color
     *            the color
     */
    public HEKEvent(String eventName, String eventDisplayName, String description, JHVEventType eventType, ImageIcon icon, Color color) {
        initLists();
        this.eventName = eventName;
        this.eventDisplayName = eventDisplayName;
        this.description = description;
        this.icon = icon;
        this.eventType = eventType;
        this.color = color;
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
    public List<JHVEventParameter> getAllEventParameters() {
        return allParameters;
    }

    @Override
    public List<JHVEventParameter> getVisibleEventParameters() {
        return allVisibleParameters;
    }

    @Override
    public List<JHVEventParameter> getVisibleNotNullEventParameters() {
        return allVisibleNotNullParameters;
    }

    @Override
    public List<JHVEventParameter> getVisibleNullEventParameters() {
        return allVisibleNullParameters;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleEventParameters() {
        return allNonVisibleParameters;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleNotNullEventParameters() {
        return allNonVisibleNotNullParameters;
    }

    @Override
    public List<JHVEventParameter> getNonVisibleNullEventParameters() {
        return allNonVisibleNullParameters;
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
    public JHVEventType getJHVEventType() {
        return eventType;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public HashMap<JHVCoordinateSystem, JHVPositionInformation> getPositioningInformation() {
        return positionInformation;
    }

    @Override
    public String getUniqueID() {
        return uniqueID;
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
        allParameters.add(parameter);
        if (configured) {
            if (visible) {
                allVisibleParameters.add(parameter);
                if (parameter.getParameterValue() == null) {
                    allVisibleNullParameters.add(parameter);
                } else {
                    allVisibleNotNullParameters.add(parameter);
                }
            } else {
                allNonVisibleParameters.add(parameter);
                if (parameter.getParameterValue() == null) {
                    allNonVisibleNullParameters.add(parameter);
                } else {
                    allNonVisibleNotNullParameters.add(parameter);
                }
            }
        }
    }

    /**
     * Sets the start date of the HEKEvent.
     *
     * @param startDate
     *            the start date
     */
    public void setStartTime(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Sets the end date of the HEKEvent.
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
        allParameters = new ArrayList<JHVEventParameter>();
        allVisibleParameters = new ArrayList<JHVEventParameter>();
        allVisibleNotNullParameters = new ArrayList<JHVEventParameter>();
        allVisibleNullParameters = new ArrayList<JHVEventParameter>();
        allNonVisibleParameters = new ArrayList<JHVEventParameter>();
        allNonVisibleNotNullParameters = new ArrayList<JHVEventParameter>();
        allNonVisibleNullParameters = new ArrayList<JHVEventParameter>();
        positionInformation = new HashMap<JHVCoordinateSystem, JHVPositionInformation>();
    }

}
