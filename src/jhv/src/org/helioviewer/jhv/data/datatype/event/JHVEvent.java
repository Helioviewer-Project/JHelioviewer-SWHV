package org.helioviewer.jhv.data.datatype.event;

import java.util.ArrayList;

public class JHVEvent {

    public final long start;
    public final long end;

    private final String eventName;

    private JHVEventParameter[] allParameters;
    private JHVEventParameter[] visibleParameters;
    private ArrayList<JHVEventParameter> visibleParametersArray;
    private ArrayList<JHVEventParameter> allParametersArray;
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

    public JHVEventParameter[] getAllEventParameters() {
        return allParameters;
    }

    public JHVEventParameter getParameter(String key) {
        int i = 0;
        while (i < allParameters.length) {
            JHVEventParameter p = allParameters[i];
            if (p.getParameterName() == key)
                return p;
            i++;
        }
        return null;
    }

    public JHVEventParameter[] getVisibleEventParameters() {
        return visibleParameters;
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

    public void addParameter(JHVEventParameter parameter, boolean visible, boolean configured, boolean full) {
        String keyString = parameter.getParameterName();
        if (!visible && !full)
            if (keyString != "cme_radiallinvel" && keyString != "event_coord1" && keyString != "cme_angularwidth") {
                return;
            }
        allParametersArray.add(parameter);
        if (configured && visible) {
            visibleParametersArray.add(parameter);
        }
    }

    public void addParameter(String keyString, String value, boolean full) {
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
        addParameter(parameter, visible, configured, full);
    }

    public void finishParams() {
        allParameters = allParametersArray.toArray(new JHVEventParameter[allParametersArray.size()]);
        allParametersArray = null;
        visibleParameters = visibleParametersArray.toArray(new JHVEventParameter[visibleParametersArray.size()]);
        visibleParametersArray = null;
    }

    public void initParams() {
        allParametersArray = new ArrayList<JHVEventParameter>();
        visibleParametersArray = new ArrayList<JHVEventParameter>();
    }
}
