package org.helioviewer.jhv.events;

import java.util.ArrayList;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.validator.routines.UrlValidator;

public class JHVEvent {

    public final long start;
    public final long end;

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

    @Nullable
    public JHVEventParameter getParameter(String key) {
        int i = 0;
        while (i < allParameters.length) {
            JHVEventParameter p = allParameters[i];
            if (p.getParameterName().equals(key))
                return p;
            i++;
        }
        return null;
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
        String keyString = parameter.getParameterName();
        if (!visible && !full)
            if (Stream.of("cme_radiallinvel", "event_coord1", "cme_angularwidth").noneMatch(s -> s.equals(keyString))) {
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
        allParameters = allParametersArray.toArray(JHVEventParameter[]::new);
        allParametersArray = new ArrayList<>();
        visibleParameters = visibleParametersArray.toArray(JHVEventParameter[]::new);

        UrlValidator urlValidator = UrlValidator.getInstance();
        // maybe should be configured
        ArrayList<JHVEventParameter> simpleVisibleParametersArray = new ArrayList<>();
        for (JHVEventParameter param : visibleParametersArray) {
            if (!urlValidator.isValid(param.getParameterValue()))
                simpleVisibleParametersArray.add(param);
        }
        simpleVisibleParameters = simpleVisibleParametersArray.toArray(JHVEventParameter[]::new);

        visibleParametersArray = new ArrayList<>();
    }

    public boolean isCactus() {
        return supplier.isCactus();
    }

}
