package org.helioviewer.jhv.events;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.validator.routines.UrlValidator;

public class JHVEvent {

    public final long start;
    public final long end;

    private JHVEventParameter[] allParameters;
    private JHVEventParameter[] visibleParameters;
    private JHVEventParameter[] simpleVisibleParameters;

    private List<JHVEventParameter> allParametersArray = new ArrayList<>();
    private List<JHVEventParameter> visibleParametersArray = new ArrayList<>();

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
            if (p.getParameterName() == key)
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

    @Nullable
    private static SWEK.Parameter parameterFromList(String name, List<SWEK.Parameter> parameterList) {
        for (SWEK.Parameter p : parameterList) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public void addParameter(String keyString, String value, boolean full) {
        boolean visible = false;
        boolean configured = false;
        String displayName;

        SWEK.Parameter p = parameterFromList(keyString, supplier.getGroup().getParameterList());
        if (p == null) {
            p = parameterFromList(keyString, supplier.getSource().generalParameters());
        }

        if (p != null) {
            configured = true;
            visible = p.visible();
            displayName = p.displayName();
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
        List<JHVEventParameter> simpleVisibleParametersArray = new ArrayList<>();
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
