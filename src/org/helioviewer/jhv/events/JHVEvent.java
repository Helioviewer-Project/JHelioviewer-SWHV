package org.helioviewer.jhv.events;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.validator.routines.UrlValidator;

public class JHVEvent {
    public final long start;
    public final long end;
    private final int id;
    private final SWEKSupplier supplier;

    private JHVEventParameter[] allParameters = new JHVEventParameter[0];
    private JHVEventParameter[] visibleParameters = new JHVEventParameter[0];
    private JHVEventParameter[] simpleVisibleParameters = new JHVEventParameter[0];

    private List<JHVEventParameter> allParametersArray = new ArrayList<>();
    private List<JHVEventParameter> visibleParametersArray = new ArrayList<>();
    private JHVPositionInformation positionInformation = null;

    public JHVEvent(SWEKSupplier _supplier, int _id, long _start, long _end) {
        supplier = _supplier;
        start = _start;
        end = _end;
        id = _id;
    }

    public JHVEventParameter[] getAllEventParameters() { return allParameters; }
    public JHVEventParameter[] getVisibleEventParameters() { return visibleParameters; }
    public JHVEventParameter[] getSimpleVisibleEventParameters() { return simpleVisibleParameters; }
    public JHVPositionInformation getPositionInformation() { return positionInformation; }
    public boolean isCactus() { return supplier.isCactus(); }
    public int getUniqueID() { return id; }
    public SWEKSupplier getSupplier() { return supplier; }

    public void addPositionInformation(JHVPositionInformation pi) {
        this.positionInformation = pi;
    }

    @Nullable
    public JHVEventParameter getParameter(String key) {
        for (JHVEventParameter p : allParameters) {
            if (p.getParameterName() == key) return p;
        }
        return null;
    }

    public void addParameter(JHVEventParameter parameter, boolean visible, boolean configured, boolean full) {
        String key = parameter.getParameterName();
        if (!visible && !full) {
            if (key != "cme_radiallinvel" && key != "event_coord1" && key != "cme_angularwidth") {
                return;
            }
        }
        allParametersArray.add(parameter);
        if (configured && visible) visibleParametersArray.add(parameter);
    }

    public void addParameter(String key, String value, boolean full) {
        SWEK.Parameter p = parameterFromList(key, supplier.getGroup().getParameterList());
        if (p == null) p = parameterFromList(key, supplier.getSource().generalParameters());

        boolean visible = (p != null) && p.visible();
        boolean configured = (p != null);
        String displayName = (p != null) ? p.displayName() : key.replace("_", " ").trim();

        addParameter(new JHVEventParameter(key, displayName, value), visible, configured, full);
    }

    private static SWEK.Parameter parameterFromList(String name, List<SWEK.Parameter> list) {
        for (SWEK.Parameter p : list) if (p.name().equalsIgnoreCase(name)) return p;
        return null;
    }

    public void finishParams() {
        allParameters = allParametersArray.toArray(new JHVEventParameter[0]);
        visibleParameters = visibleParametersArray.toArray(new JHVEventParameter[0]);

        UrlValidator uv = UrlValidator.getInstance();
        List<JHVEventParameter> simple = new ArrayList<>();
        for (JHVEventParameter p : visibleParameters) {
            if (!uv.isValid(p.getParameterValue())) simple.add(p);
        }
        simpleVisibleParameters = simple.toArray(new JHVEventParameter[0]);

        allParametersArray = new ArrayList<>();
        visibleParametersArray = new ArrayList<>();
    }
}
