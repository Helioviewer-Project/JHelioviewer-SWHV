package org.helioviewer.jhv.data.event;

import java.util.ArrayList;

import org.helioviewer.jhv.base.Regex;

public class JHVEvent {

    public final long start;
    public final long end;

    private final String eventName;

    private JHVEventParameter[] allParameters;
    private JHVEventParameter[] visibleParameters;
    private JHVEventParameter[] simpleVisibleParameters;

    private ArrayList<JHVEventParameter> allParametersArray = new ArrayList<>();
    private ArrayList<JHVEventParameter> visibleParametersArray = new ArrayList<>();

    private final SWEKSupplier supplier;
    private JHVPositionInformation positionInformation = null;
    private final int id;

    public JHVEvent(SWEKSupplier _supplier, int _id, long _start, long _end) {
        supplier = _supplier;
        eventName = _supplier.getGroup().getName();
        start = _start;
        end = _end;
        id = _id;
    }

    public JHVEventParameter[] getAllEventParameters() {
        return allParameters;
    }

    public JHVEventParameter[] getVisibleEventParameters() {
        return visibleParameters;
    }

    public JHVEventParameter[] getSimpleVisibleEventParameters() {
        return simpleVisibleParameters;
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

    public String getName() {
        return eventName;
    }

    public SWEKSupplier getSupplier() {
        return supplier;
    }

    public JHVPositionInformation getPositionInformation() {
        return positionInformation;
    }

    public void addPositionInformation(JHVPositionInformation _positionInformation) {
        positionInformation = _positionInformation;
    }

    public int getUniqueID() {
        return id;
    }

    public void addParameter(JHVEventParameter parameter, boolean visible, boolean configured, boolean full) {
        String keyString = parameter.getParameterName(); // interned
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

        SWEKParameter p = supplier.getGroup().getParameter(keyString);
        if (p == null) {
            p = supplier.getSource().getParameter(keyString);
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
        allParametersArray = new ArrayList<>();
        visibleParameters = visibleParametersArray.toArray(new JHVEventParameter[visibleParametersArray.size()]);

        // maybe should be configured
        ArrayList<JHVEventParameter> simpleVisibleParametersArray = new ArrayList<>();
        for (JHVEventParameter param : visibleParametersArray) {
            if (!Regex.WEB_URL.matcher(param.getParameterValue()).matches())
                simpleVisibleParametersArray.add(param);
        }
        simpleVisibleParameters = simpleVisibleParametersArray.toArray(new JHVEventParameter[simpleVisibleParametersArray.size()]);

        visibleParametersArray = new ArrayList<>();
    }

}
