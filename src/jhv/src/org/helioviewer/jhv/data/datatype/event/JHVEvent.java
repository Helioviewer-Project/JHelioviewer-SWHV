package org.helioviewer.jhv.data.datatype.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.base.astronomy.Position;

public class JHVEvent {

    private final Date startDate;
    private final Date endDate;
    private final String eventName;
    private final String eventDisplayName;
    private Map<String, JHVEventParameter> allParameters;
    private Map<String, JHVEventParameter> allVisibleParameters;
    private Map<String, JHVEventParameter> allVisibleNotNullParameters;
    private Map<String, JHVEventParameter> allVisibleNullParameters;
    private Map<String, JHVEventParameter> allNonVisibleParameters;
    private Map<String, JHVEventParameter> allNonVisibleNotNullParameters;
    private Map<String, JHVEventParameter> allNonVisibleNullParameters;
    private final JHVEventType eventType;
    private JHVPositionInformation positionInformation = JHVPositionInformation.NULLINFO;
    private Position.Q earthPosition = null;
    private Integer id;

    public JHVEvent(String _eventName, String _eventDisplayName, JHVEventType _eventType, int _id, Date _start, Date _end) {
        initLists();
        eventName = _eventName;
        eventDisplayName = _eventDisplayName;
        eventType = _eventType;
        startDate = _start;
        endDate = _end;
        id = _id;
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

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Map<String, JHVEventParameter> getAllEventParameters() {
        return allParameters;
    }

    public Map<String, JHVEventParameter> getVisibleEventParameters() {
        return allVisibleParameters;
    }

    public Map<String, JHVEventParameter> getVisibleNotNullEventParameters() {
        return allVisibleNotNullParameters;
    }

    public Map<String, JHVEventParameter> getVisibleNullEventParameters() {
        return allVisibleNullParameters;
    }

    public Map<String, JHVEventParameter> getNonVisibleEventParameters() {
        return allNonVisibleParameters;
    }

    public Map<String, JHVEventParameter> getNonVisibleNotNullEventParameters() {
        return allNonVisibleNotNullParameters;
    }

    public Map<String, JHVEventParameter> getNonVisibleNullEventParameters() {
        return allNonVisibleNullParameters;
    }

    public String getName() {
        return eventName;
    }

    public String getDisplayName() {
        return eventDisplayName;
    }

    public JHVEventType getJHVEventType() {
        return eventType;
    }

    public JHVPositionInformation getPositioningInformation() {
        return positionInformation;
    }

    public Integer getUniqueID() {
        return id;
    }

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

    public void addJHVPositionInformation(JHVPositionInformation positionInformation) {
        this.positionInformation = positionInformation;
    }

    public void addEarthPosition(Position.Q p) {
        earthPosition = p;
    }

    public Position.Q getEarthPosition() {
        return earthPosition;
    }

    public void addParameter(String keyString, String value) {
        boolean visible = false;
        boolean configured = false;
        String displayName;
        SWEKParameter p = eventType.getEventType().getParameter(keyString);
        if (p == null) {
            p = eventType.getSupplier().getSource().getParameter(keyString);
        }
        if (p != null) {
            configured = true;
            visible = p.isDefaultVisible();
            displayName = p.getParameterDisplayName();
        }
        else {
            displayName = keyString.replaceAll("_", " ").trim();
        }

        JHVEventParameter parameter = new JHVEventParameter(keyString, displayName, value);
        addParameter(parameter, visible, configured);
    }

}
