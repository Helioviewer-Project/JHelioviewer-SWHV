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
    private JHVPositionInformation positionInformation;
    private Integer id;

    public HEKEvent(String _eventName, String _eventDisplayName, JHVEventType _eventType, int _id, Date _start, Date _end) {
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

    public void addJHVPositionInformation(JHVPositionInformation positionInformation) {
        this.positionInformation = positionInformation;
    }

}
