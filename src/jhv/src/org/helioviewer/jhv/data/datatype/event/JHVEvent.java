package org.helioviewer.jhv.data.datatype.event;

import java.util.HashMap;
import java.util.Map;

public class JHVEvent {

    public final long start;
    public final long end;

    private final String eventName;

    private final Map<String, JHVEventParameter> allParameters = new HashMap<String, JHVEventParameter>();
    private final Map<String, JHVEventParameter> allVisibleParameters = new HashMap<String, JHVEventParameter>();

    private final JHVEventType eventType;
    private JHVPositionInformation positionInformation = null;
    private Integer id;

    public JHVEvent(JHVEventType _eventType, int _id, long _start, long _end) {
        eventType = _eventType;
        eventName = _eventType.getEventType().getEventName();
        start = _start;
        end = _end;
        id = _id;
    }

    public Map<String, JHVEventParameter> getAllEventParameters() {
        return allParameters;
    }

    public Map<String, JHVEventParameter> getVisibleEventParameters() {
        return allVisibleParameters;
    }

    public String getName() {
        return eventName;
    }

    public JHVEventType getJHVEventType() {
        return eventType;
    }

    public JHVPositionInformation getPositionInformation() {
        return positionInformation;
    }

    public void addPositionInformation(JHVPositionInformation positionInformation) {
        this.positionInformation = positionInformation;
    }

    public Integer getUniqueID() {
        return id;
    }

    public void setUniqueID(Integer id) {
        this.id = id;
    }

    // configured = was the event in the configuration file
    public void addParameter(JHVEventParameter parameter, boolean visible, boolean configured) {
        String name = parameter.getParameterName();
        allParameters.put(name, parameter);
        if (configured && visible) {
            allVisibleParameters.put(name, parameter);
        }
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
        } else {
            displayName = keyString.replaceAll("_", " ").trim();
        }

        JHVEventParameter parameter = new JHVEventParameter(keyString, displayName, value);
        addParameter(parameter, visible, configured);
    }

}
