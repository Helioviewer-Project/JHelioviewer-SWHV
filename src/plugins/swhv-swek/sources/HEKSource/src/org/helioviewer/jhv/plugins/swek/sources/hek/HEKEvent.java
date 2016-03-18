package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    /** the event name */
    private final String eventName;

    /** the event display name */
    private final String eventDisplayName;

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

    /** List with positioning information for this event */
    private JHVPositionInformation positionInformation;

    private Integer id;

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
    public HEKEvent(String eventName, String eventDisplayName, JHVEventType eventType) {
        initLists();
        this.eventName = eventName;
        this.eventDisplayName = eventDisplayName;
        this.eventType = eventType;
    }

    private void initLists() {
        allParameters = new HashMap<String, JHVEventParameter>();
        allVisibleParameters = new HashMap<String, JHVEventParameter>();
        allVisibleNotNullParameters = new HashMap<String, JHVEventParameter>();
        allVisibleNullParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleNotNullParameters = new HashMap<String, JHVEventParameter>();
        allNonVisibleNullParameters = new HashMap<String, JHVEventParameter>();
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
    public String getName() {
        return eventName;
    }

    @Override
    public String getDisplayName() {
        return eventDisplayName;
    }

    @Override
    public JHVEventType getJHVEventType() {
        return eventType;
    }

    @Override
    public JHVPositionInformation getPositioningInformation() {
        return positionInformation;
    }

    @Override
    public Integer getUniqueID() {
        return id;
    }

    @Override
    public void setUniqueID(Integer id) {
        this.id = id;
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
                if (parameter.getParameterValue() == null || (parameter.getParameterValue().trim().length() == 0)) {
                    allVisibleNullParameters.put(parameter.getParameterName(), parameter);
                } else {
                    allVisibleNotNullParameters.put(parameter.getParameterName(), parameter);
                }
            } else {
                allNonVisibleParameters.put(parameter.getParameterName(), parameter);
                if (parameter.getParameterValue() == null || (parameter.getParameterValue().trim().length() == 0)) {
                    allNonVisibleNullParameters.put(parameter.getParameterName(), parameter);
                } else {
                    allNonVisibleNotNullParameters.put(parameter.getParameterName(), parameter);
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
     * Adds position information to the HEKEvent.
     *
     * @param positionInformation
     *            the position information to add
     */
    public void addJHVPositionInformation(JHVPositionInformation positionInformation) {
        this.positionInformation = positionInformation;
    }

}
